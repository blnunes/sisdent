package br.com.itbn.sisdent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodRequest;
import br.com.itbn.sisdent.dto.AppointmentBlockedPeriodResponse;
import br.com.itbn.sisdent.model.AppointmentBlockedPeriod;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentBlockedPeriodRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AppointmentBlockedPeriodServiceTest {
    private final UUID organizationId = UUID.randomUUID();
    private final UUID clinicUnitId = UUID.randomUUID();
    private final UUID practitionerId = UUID.randomUUID();
    private final UUID blockedPeriodId = UUID.randomUUID();
    private final Instant startAt = Instant.parse("2030-01-01T09:00:00Z");
    private final Instant endAt = Instant.parse("2030-01-01T10:00:00Z");

    @Mock
    private AppointmentBlockedPeriodRepository blockedPeriods;
    @Mock
    private PractitionerRepository practitioners;
    @Mock
    private ScopeAuthorizationService authorization;
    @Mock
    private ClinicUnit clinic;
    @Mock
    private Practitioner practitioner;

    private AppointmentBlockedPeriodService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentBlockedPeriodService(blockedPeriods, practitioners, authorization);
    }

    private void stubClinic() {
        when(authorization.requireClinicInOrganization(organizationId, clinicUnitId)).thenReturn(clinic);
        when(clinic.getGlobalId()).thenReturn(clinicUnitId);
    }

    @Test
    void createsClinicWideBlockWithManagementScopeAndNoPractitioner() {
        stubClinic();
        when(clinic.getOrganization()).thenReturn(new Organization("Northstar"));
        AppointmentBlockedPeriodRequest request = new AppointmentBlockedPeriodRequest(
                clinicUnitId, null, startAt, endAt);
        when(blockedPeriods.save(org.mockito.ArgumentMatchers.any(AppointmentBlockedPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentBlockedPeriodResponse response = service.create(organizationId, request);

        verify(authorization).requireAppointmentManagement(organizationId, clinicUnitId);
        assertThat(response.clinicUnitId()).isEqualTo(clinicUnitId);
        assertThat(response.practitionerId()).isNull();
        assertThat(response.startAt()).isEqualTo(startAt);
    }

    @Test
    void createsPractitionerBlockOnlyForAnActivePractitionerInTheOrganization() {
        stubClinic();
        when(clinic.getOrganization()).thenReturn(new Organization("Northstar"));
        AppointmentBlockedPeriodRequest request = new AppointmentBlockedPeriodRequest(
                clinicUnitId, practitionerId, startAt, endAt);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId))
                .thenReturn(Optional.of(practitioner));
        when(practitioner.isActive()).thenReturn(true);
        when(practitioner.getGlobalId()).thenReturn(practitionerId);
        when(blockedPeriods.save(org.mockito.ArgumentMatchers.any(AppointmentBlockedPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentBlockedPeriodResponse response = service.create(organizationId, request);

        assertThat(response.practitionerId()).isEqualTo(practitionerId);
    }

    @Test
    void rejectsInvalidRangeWithoutPersisting() {
        when(authorization.requireClinicInOrganization(organizationId, clinicUnitId)).thenReturn(clinic);
        AppointmentBlockedPeriodRequest invalidRange = new AppointmentBlockedPeriodRequest(
                clinicUnitId, null, endAt, startAt);

        assertThatThrownBy(() -> service.create(organizationId, invalidRange))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInactivePractitionerWithoutPersisting() {
        when(authorization.requireClinicInOrganization(organizationId, clinicUnitId)).thenReturn(clinic);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId))
                .thenReturn(Optional.of(practitioner));
        when(practitioner.isActive()).thenReturn(false);
        AppointmentBlockedPeriodRequest inactivePractitioner = new AppointmentBlockedPeriodRequest(
                clinicUnitId, practitionerId, startAt, endAt);

        assertThatThrownBy(() -> service.create(organizationId, inactivePractitioner))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rejectsStaleOrCrossClinicUpdateAndDelete() {
        stubClinic();
        AppointmentBlockedPeriod blockedPeriod = org.mockito.Mockito.mock(AppointmentBlockedPeriod.class);
        when(blockedPeriods.findByGlobalIdAndOrganization_GlobalId(blockedPeriodId, organizationId))
                .thenReturn(Optional.of(blockedPeriod));
        when(blockedPeriod.getClinicUnit()).thenReturn(clinic);
        when(blockedPeriod.getVersion()).thenReturn(2L);
        AppointmentBlockedPeriodRequest request = new AppointmentBlockedPeriodRequest(
                clinicUnitId, null, startAt, endAt);

        assertThatThrownBy(() -> service.update(organizationId, blockedPeriodId, 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        UUID foreignClinicId = UUID.randomUUID();
        assertThatThrownBy(() -> service.delete(organizationId, foreignClinicId, blockedPeriodId, 2L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listsOnlyTheSelectedClinicAndFiniteRange() {
        stubClinic();
        AppointmentBlockedPeriod blockedPeriod = org.mockito.Mockito.mock(AppointmentBlockedPeriod.class);
        when(blockedPeriod.getGlobalId()).thenReturn(blockedPeriodId);
        when(blockedPeriod.getClinicUnit()).thenReturn(clinic);
        when(blockedPeriod.getStartAt()).thenReturn(startAt);
        when(blockedPeriod.getEndAt()).thenReturn(endAt);
        when(blockedPeriod.getVersion()).thenReturn(0L);
        when(blockedPeriods.findScopedOverlapping(organizationId, clinicUnitId, startAt, endAt))
                .thenReturn(List.of(blockedPeriod));

        List<AppointmentBlockedPeriodResponse> response = service.list(organizationId, clinicUnitId, startAt, endAt);

        assertThat(response).hasSize(1);
        verify(authorization).requireAppointmentManagement(organizationId, clinicUnitId);
    }
}
