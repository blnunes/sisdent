package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AccountCreateRequest;
import br.com.itbn.sisdent.dto.AccountLifecycleRequest;
import br.com.itbn.sisdent.dto.AccountPlatformAdministratorRequest;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {
    @Mock AccountRepository accounts;
    @Mock PersonRepository persons;
    @Mock MembershipRepository memberships;
    @Mock PasswordEncoder passwords;
    @Mock PageableFactory pages;
    @Mock ScopeAuthorizationService authorization;
    @Mock CurrentAccountService current;
    @InjectMocks AccountManagementService service;

    @Test
    void createsAndReadsTheCurrentAccount() {
        AccountCreateRequest request = new AccountCreateRequest("Ana", " ANA@example.com ", "password-123");
        when(accounts.existsByEmail("ana@example.com")).thenReturn(false);
        when(persons.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwords.encode("password-123")).thenReturn("encoded");
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.findAllByAccount_IdOrderByOrganization_NameAscClinicUnit_NameAsc(any())).thenReturn(List.of());

        assertThat(service.create(request).email()).isEqualTo("ana@example.com");
        Account currentAccount = new Account(new Person("Current"), "current@example.com", "encoded", false);
        when(current.require()).thenReturn(currentAccount);
        assertThat(service.currentAccount().email()).isEqualTo("current@example.com");
    }

    @Test
    void changesAccountLifecycleAndRejectsInvalidVersions() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "encoded", false);
        when(accounts.findByGlobalId(account.getGlobalId())).thenReturn(Optional.of(account));
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberships.findAllByAccount_IdOrderByOrganization_NameAscClinicUnit_NameAsc(any())).thenReturn(List.of());

        assertThat(service.changeLifecycle(account.getGlobalId(), new AccountLifecycleRequest(false, 0L)).active()).isFalse();
        assertThatThrownBy(() -> service.changeLifecycle(account.getGlobalId(), new AccountLifecycleRequest(true, 1L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("changed by another request");
    }

    @Test
    void protectsTheLastActivePlatformAdministrator() {
        Account account = new Account(new Person("Admin"), "admin@example.com", "encoded", true);
        when(accounts.findLockedByGlobalId(account.getGlobalId())).thenReturn(Optional.of(account));
        when(accounts.countByPlatformAdministratorTrueAndActiveTrue()).thenReturn(1L);

        assertThatThrownBy(() -> service.changePlatformAdministrator(account.getGlobalId(),
                new AccountPlatformAdministratorRequest(false, 0L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("At least one active");
    }
}
