package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PractitionerRequest;
import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PractitionerServiceTest {
    @Mock PractitionerRepository practitioners;
    @Mock OrganizationRepository organizations;
    @Mock AccountRepository accounts;
    @Mock SpecialityRepository specialities;
    @Mock MembershipRepository memberships;
    @Mock ScopeAuthorizationService authorization;
    @InjectMocks PractitionerService service;

    private final UUID organizationId = UUID.randomUUID();
    private final UUID clinicId = UUID.randomUUID();

    @Test
    void listsPractitionersForAppointmentReaders() {
        when(practitioners.findAllByOrganization_GlobalIdOrderByDisplayName(organizationId)).thenReturn(List.of());

        assertThat(service.list(organizationId, clinicId)).isEmpty();

        verify(authorization).requireAppointmentRead(organizationId, clinicId);
    }

    @Test
    void fallsBackToPractitionerManagementWhenAppointmentReadIsDenied() {
        doThrow(new AccessDeniedException("denied")).when(authorization).requireAppointmentRead(organizationId, clinicId);
        when(practitioners.findAllByOrganization_GlobalIdOrderByDisplayName(organizationId)).thenReturn(List.of());

        assertThat(service.list(organizationId, clinicId)).isEmpty();

        verify(authorization).requirePractitionerManagement(organizationId, clinicId);
    }

    @Test
    void createsPractitionersOnlyForKnownOrganizationsAndActiveSpecialities() {
        PractitionerRequest request = new PractitionerRequest(" Dr Ana ", " REG-1 ", null, Set.of(1L));
        assertThatThrownBy(() -> service.create(organizationId, request)).isInstanceOf(ResponseStatusException.class);

        Organization organization = new Organization("Alpha");
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        assertThatThrownBy(() -> service.create(organizationId, request)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unknown speciality");

        Speciality speciality = org.mockito.Mockito.mock(Speciality.class);
        when(speciality.getStatus()).thenReturn(CatalogStatus.ACTIVE);
        when(specialities.findById(1L)).thenReturn(Optional.of(speciality));
        when(practitioners.save(any(Practitioner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create(organizationId, request).displayName()).isEqualTo("Dr Ana");
    }

    @Test
    void updatesAndDeactivatesOnlyPractitionersInTheOrganization() {
        UUID practitionerId = UUID.randomUUID();
        Practitioner practitioner = org.mockito.Mockito.mock(Practitioner.class);
        when(practitioners.findByGlobalIdAndOrganization_GlobalId(practitionerId, organizationId))
                .thenReturn(Optional.of(practitioner));
        when(practitioner.getSpecialities()).thenReturn(Set.of());
        PractitionerRequest request = new PractitionerRequest("Ana", null, null, Set.of());

        assertThat(service.update(organizationId, practitionerId, request)).isNotNull();
        service.deactivate(organizationId, practitionerId);

        verify(practitioner).update(null, "Ana", null, Set.of());
        verify(practitioner).deactivate();
    }
}
