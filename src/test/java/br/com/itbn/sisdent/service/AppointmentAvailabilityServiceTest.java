package br.com.itbn.sisdent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.itbn.sisdent.dto.AppointmentAvailabilityResponse.Category;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentBlockedPeriod;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.ClinicUnitBreak;
import br.com.itbn.sisdent.model.ClinicUnitWorkingHours;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.repository.AppointmentBlockedPeriodRepository;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.ClinicUnitBreakRepository;
import br.com.itbn.sisdent.repository.ClinicUnitWorkingHoursRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AppointmentAvailabilityServiceTest {
    @Mock
    ClinicUnitWorkingHoursRepository hours;

    @Mock
    ClinicUnitBreakRepository breaks;

    @Mock
    AppointmentBlockedPeriodRepository blocked;

    @Mock
    AppointmentRepository appointments;

    @Mock
    PractitionerRepository practitioners;

    @Mock
    ScopeAuthorizationService authorization;

    @InjectMocks
    AppointmentAvailabilityService service;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID clinicId = UUID.randomUUID();
    private final UUID practitionerId = UUID.randomUUID();
    private final Instant start = Instant.parse("2026-03-29T08:00:00Z");
    private final Instant end = Instant.parse("2026-03-29T09:00:00Z");
    private ClinicUnit clinic;
    private Practitioner practitioner;

    @BeforeEach
    void setUp() {
        clinic = mock(ClinicUnit.class);
        practitioner = mock(Practitioner.class);
        lenient().when(practitioner.isActive()).thenReturn(true);
        lenient().when(clinic.getId()).thenReturn(2L);
        lenient().when(clinic.getGlobalId()).thenReturn(clinicId);
        lenient().when(clinic.getTimezone()).thenReturn("Europe/Lisbon");
        lenient().when(practitioner.getId()).thenReturn(3L);
        lenient().when(practitioner.getGlobalId()).thenReturn(practitionerId);
        lenient().when(hours.findAllByClinicUnit_Id(2L))
                .thenReturn(List.of(new ClinicUnitWorkingHours(clinic, 7, 0, 1440)));
        lenient().when(breaks.findAllByClinicUnit_Id(2L)).thenReturn(List.of());
        lenient().when(blocked.findOverlapping(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(appointments.findScheduledOverlapping(any(), any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(appointments.hasOverlap(anyLong(), any(), any(), isNull())).thenReturn(false);
    }

    @Test
    void returnsWorkingHoursAndOccupiedBackgroundIntervalsAcrossDst() {
        Appointment occupied = new Appointment(new Organization("A"), clinic, mock(PatientOrganizationLink.class), practitioner,
                start, end, "Europe/Lisbon");
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));
        when(appointments.findScheduledOverlapping(organizationId, clinicId, practitionerId, start, end)).thenReturn(List.of(occupied));

        assertThat(service.list(organizationId, clinicId, start, end, List.of(practitionerId)))
                .extracting(interval -> interval.category())
                .containsExactlyInAnyOrder(Category.WORKING_HOURS, Category.OCCUPIED);
    }

    @Test
    void returnsWorkingHoursAndBreakIntervalsForEachMatchingDay() {
        Instant multiDayStart = Instant.parse("2026-03-28T08:00:00Z");
        Instant multiDayEnd = Instant.parse("2026-03-30T10:00:00Z");
        when(hours.findAllByClinicUnit_Id(2L)).thenReturn(List.of(
                new ClinicUnitWorkingHours(clinic, 6, 0, 1440),
                new ClinicUnitWorkingHours(clinic, 7, 0, 1440),
                new ClinicUnitWorkingHours(clinic, 1, 0, 1440)));
        when(breaks.findAllByClinicUnit_Id(2L)).thenReturn(List.of(new ClinicUnitBreak(clinic, 7, 480, 540)));
        when(authorization.requireClinicInOrganization(organizationId, clinicId)).thenReturn(clinic);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId)).thenReturn(Optional.of(practitioner));

        assertThat(service.list(organizationId, clinicId, multiDayStart, multiDayEnd, List.of(practitionerId)))
                .extracting(interval -> interval.category())
                .containsExactlyInAnyOrder(
                        Category.WORKING_HOURS,
                        Category.WORKING_HOURS,
                        Category.WORKING_HOURS,
                        Category.BREAK);
    }

    @Test
    void rejectsOutsideHoursBreaksAndInvalidRanges() {
        when(hours.findAllByClinicUnit_Id(2L)).thenReturn(List.of(new ClinicUnitWorkingHours(clinic, 7, 600, 660)));
        assertThatThrownBy(() -> service.requireAvailable(organizationId, clinic, practitioner, start, end, null))
                .isInstanceOf(SchedulingConflictException.class);

        when(hours.findAllByClinicUnit_Id(2L)).thenReturn(List.of(new ClinicUnitWorkingHours(clinic, 7, 0, 1440)));
        when(breaks.findAllByClinicUnit_Id(2L)).thenReturn(List.of(new ClinicUnitBreak(clinic, 7, 480, 600)));
        assertThatThrownBy(() -> service.requireAvailable(organizationId, clinic, practitioner, start, end, null))
                .isInstanceOf(SchedulingConflictException.class);
        assertThatThrownBy(() -> service.list(organizationId, clinicId, end, start, List.of()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsBlockedAndOccupiedIntervals() {
        when(blocked.findOverlapping(organizationId, clinicId, practitionerId, start, end))
                .thenReturn(List.of(mock(AppointmentBlockedPeriod.class)));
        assertThatThrownBy(() -> service.requireAvailable(organizationId, clinic, practitioner, start, end, null))
                .isInstanceOf(SchedulingConflictException.class);

        when(blocked.findOverlapping(organizationId, clinicId, practitionerId, start, end)).thenReturn(List.of());
        when(appointments.hasOverlap(3L, start, end, null)).thenReturn(true);
        assertThatThrownBy(() -> service.requireAvailable(organizationId, clinic, practitioner, start, end, null))
                .isInstanceOf(SchedulingConflictException.class);
    }
}
