package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ClinicUnitRequest;
import br.com.itbn.sisdent.dto.MembershipRequest;
import br.com.itbn.sisdent.dto.OrganizationRequest;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {
    @Mock OrganizationRepository organizations;
    @Mock ClinicUnitRepository clinics;
    @Mock MembershipRepository memberships;
    @Mock AccountRepository accounts;
    @Mock PersonRepository persons;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Mock ScopeAuthorizationService authorization;
    @InjectMocks OrganizationService service;

    @Test
    void createsAndListsActiveOrganizationsForPlatformAdministrator() {
        Organization first = new Organization("Alpha");
        when(organizations.saveAndFlush(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizations.findAll()).thenReturn(List.of(first));

        assertThat(service.createOrganization(new OrganizationRequest("Beta")).name()).isEqualTo("Beta");
        assertThat(service.listOrganizationsForPlatform()).extracting(response -> response.name())
                .containsExactly("Alpha");
        verify(authorization, times(2)).requirePlatformAdministrator();
    }

    @Test
    void createsClinicUnitAndScopesTheResponseToItsOrganization() {
        Organization organization = new Organization("Alpha");
        UUID organizationId = organization.getGlobalId();
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        when(clinics.saveAndFlush(any(ClinicUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createClinicUnit(organizationId, new ClinicUnitRequest("Central"));

        assertThat(response.organizationId()).isEqualTo(organizationId);
        assertThat(response.name()).isEqualTo("Central");
        verify(authorization).requireOrganizationAdministration(organizationId);
    }

    @Test
    void createsAccountAndOrganizationWideMembershipWhenNoAccountExists() {
        Organization organization = new Organization("Alpha");
        UUID organizationId = organization.getGlobalId();
        MembershipRequest request = new MembershipRequest(" Member@Example.com ", "Member", "password-123", null,
                MembershipRole.ORGANIZATION_ADMIN);
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        when(accounts.findByEmail("member@example.com")).thenReturn(Optional.empty());
        when(persons.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwords.encode("password-123")).thenReturn("encoded");
        when(memberships.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(any(), any())).thenReturn(false);
        when(memberships.saveAndFlush(any(Membership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.addMembership(organizationId, request);

        assertThat(response.role()).isEqualTo(MembershipRole.ORGANIZATION_ADMIN);
        ArgumentCaptor<Account> account = ArgumentCaptor.forClass(Account.class);
        verify(accounts).save(account.capture());
        assertThat(account.getValue().getEmail()).isEqualTo("member@example.com");
        assertThat(account.getValue().getAccountManagementOrganization()).isSameAs(organization);
    }

    @Test
    void rejectsDuplicateMembershipInTheSameScope() {
        Organization organization = new Organization("Alpha");
        UUID organizationId = organization.getGlobalId();
        Account account = new Account(new Person("Member"), "member@example.com", "encoded", false);
        MembershipRequest request = new MembershipRequest("member@example.com", "Member", "password-123", null,
                MembershipRole.READ_ONLY);
        when(organizations.findByGlobalId(organizationId)).thenReturn(Optional.of(organization));
        when(accounts.findByEmail("member@example.com")).thenReturn(Optional.of(account));
        when(memberships.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.addMembership(organizationId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }
}
