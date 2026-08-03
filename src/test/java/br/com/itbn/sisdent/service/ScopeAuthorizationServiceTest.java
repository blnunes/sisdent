package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScopeAuthorizationServiceTest {
    @Mock CurrentAccountService currentAccountService;
    @Mock MembershipRepository memberships;
    @Mock ClinicUnitRepository clinics;
    @InjectMocks ScopeAuthorizationService service;

    private final Organization organization = new Organization("Alpha");
    private final ClinicUnit clinic = new ClinicUnit(organization, "Central");
    private final Account account = new Account(new Person("Member"), "member@example.com", "encoded", false);

    @BeforeEach
    void setUp() {
        lenient().when(currentAccountService.require()).thenReturn(account);
    }

    @Test
    void grantsAndRejectsPlatformAdministration() {
        assertThatThrownBy(service::requirePlatformAdministrator).isInstanceOf(AccessDeniedException.class);
        assertThat(service.isPlatformAdministrator()).isFalse();

        Account platformAdministrator = new Account(new Person("Admin"), "admin@example.com", "encoded", true);
        when(currentAccountService.require()).thenReturn(platformAdministrator);

        service.requirePlatformAdministrator();
        assertThat(service.isPlatformAdministrator()).isTrue();
    }

    @Test
    void grantsReadForOrganizationAndMatchingClinicMembershipsOnly() {
        grant(MembershipRole.READ_ONLY, null);
        service.requireRead(organization.getGlobalId(), null);
        service.requireRead(organization.getGlobalId(), clinic.getGlobalId());

        grant(MembershipRole.READ_ONLY, clinic);
        service.requireRead(organization.getGlobalId(), clinic.getGlobalId());
        assertThatThrownBy(() -> service.requireRead(organization.getGlobalId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void appliesWriteAndOperationalRoleRules() {
        grant(MembershipRole.MANAGER, null);
        service.requireWrite(organization.getGlobalId(), null);
        service.requirePractitionerManagement(organization.getGlobalId(), null);
        service.requireAppointmentManagement(organization.getGlobalId(), null);

        grant(MembershipRole.READ_ONLY, null);
        assertThatThrownBy(() -> service.requireWrite(organization.getGlobalId(), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requirePractitionerManagement(organization.getGlobalId(), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireAppointmentManagement(organization.getGlobalId(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void appliesAppointmentReadRoles() {
        for (MembershipRole role : List.of(MembershipRole.ORGANIZATION_ADMIN, MembershipRole.MANAGER,
                MembershipRole.APPOINTMENT_MANAGER, MembershipRole.APPOINTMENT_READER, MembershipRole.READ_ONLY)) {
            grant(role, null);
            service.requireAppointmentRead(organization.getGlobalId(), null);
        }
        grant(MembershipRole.CLINICAL_READER, null);
        assertThatThrownBy(() -> service.requireAppointmentRead(organization.getGlobalId(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void appliesClinicalReadAuthorAndManagementRules() {
        for (MembershipRole role : List.of(MembershipRole.ORGANIZATION_ADMIN, MembershipRole.CLINICAL_READER,
                MembershipRole.CLINICAL_AUTHOR, MembershipRole.CLINICAL_MANAGER)) {
            grant(role, null);
            service.requireClinicalRead(organization.getGlobalId(), null);
        }

        grant(MembershipRole.CLINICAL_MANAGER, null);
        service.requireClinicalAuthor(organization.getGlobalId(), null);
        service.requireClinicalManagement(organization.getGlobalId(), null);

        grant(MembershipRole.CLINICAL_AUTHOR, null);
        assertThatThrownBy(() -> service.requireClinicalManagement(organization.getGlobalId(), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void limitsOrganizationAndAccountAdministrationToOrganizationAdministrators() {
        grant(MembershipRole.ORGANIZATION_ADMIN, null);
        service.requireOrganizationAdministration(organization.getGlobalId());

        account.assignAccountManagementOrganizationIfAbsent(organization);
        service.requireAccountAdministration(organization.getGlobalId());

        assertThatThrownBy(() -> service.requireAccountAdministration(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void platformAdministratorBypassesOrganizationAndAccountAdministrationChecks() {
        Account platformAdministrator = new Account(new Person("Admin"), "admin@example.com", "encoded", true);
        when(currentAccountService.require()).thenReturn(platformAdministrator);

        service.requireOrganizationAdministration(organization.getGlobalId());
        service.requireAccountAdministration(organization.getGlobalId());
    }

    @Test
    void resolvesOnlyClinicUnitsFromTheRequestedOrganization() {
        when(clinics.findByGlobalId(clinic.getGlobalId())).thenReturn(Optional.of(clinic));
        assertThat(service.requireClinicInOrganization(organization.getGlobalId(), clinic.getGlobalId())).isSameAs(clinic);

        assertThatThrownBy(() -> service.requireClinicInOrganization(UUID.randomUUID(), clinic.getGlobalId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.requireClinicInOrganization(organization.getGlobalId(), UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class);
    }

    private void grant(MembershipRole role, ClinicUnit clinicUnit) {
        when(memberships.findAllByAccount_IdAndOrganization_GlobalIdAndActiveTrue(any(), eq(organization.getGlobalId())))
                .thenReturn(List.of(new Membership(account, organization, clinicUnit, role)));
    }
}
