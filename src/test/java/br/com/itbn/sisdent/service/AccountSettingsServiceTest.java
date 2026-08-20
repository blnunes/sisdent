package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.dto.UpdateOwnPreferredLanguageRequest;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSettingsServiceTest {
    @Mock CurrentAccountService currentAccountService;
    @Mock PersonRepository persons;
    @Mock AccountRepository accounts;
    @Mock PasswordEncoder passwords;
    @InjectMocks AccountSettingsService service;

    @Test
    void readsAndUpdatesOnlyTheAuthenticatedAccountsProfile() {
        Account account = new Account(new Person(" Ana "), "ana@example.com", "stored", false);
        when(currentAccountService.require()).thenReturn(account);
        when(persons.saveAndFlush(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.current()).extracting("displayName", "email").containsExactly("Ana", "ana@example.com");
        assertThat(service.updateProfile(new UpdateOwnProfileRequest("  Ana Silva  ", 0L)).displayName()).isEqualTo("Ana Silva");
        verify(persons).saveAndFlush(account.getPerson());
        verify(currentAccountService, times(2)).require();
        verifyNoInteractions(accounts, passwords);
    }

    @Test
    void rejectsBlankDisplayNames() {
        when(currentAccountService.require()).thenReturn(new Account(new Person("Ana"), "ana@example.com", "stored", false));
        assertThatThrownBy(() -> service.updateProfile(new UpdateOwnProfileRequest("  ", 0L)))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(persons);
    }

    @Test
    void changesPasswordOnlyAfterCheckingCurrentPasswordAndPersistsAnEncodedValue() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "old-hash", false);
        when(currentAccountService.require()).thenReturn(account);
        when(passwords.matches("old-password", "old-hash")).thenReturn(true);
        when(passwords.encode("new-password")).thenReturn("new-hash");
        when(accounts.saveAndFlush(account)).thenReturn(account);

        service.changePassword(new ChangeOwnPasswordRequest("old-password", "new-password"));

        assertThat(account.getPassword()).isEqualTo("new-hash");
        verify(passwords).matches("old-password", "old-hash");
        verify(passwords).encode("new-password");
        verify(accounts).saveAndFlush(account);
        verifyNoInteractions(persons);
    }

    @Test
    void rejectsInvalidCurrentAndNewPasswordsWithoutPersisting() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "old-hash", false);
        when(currentAccountService.require()).thenReturn(account);
        when(passwords.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(new ChangeOwnPasswordRequest("wrong", "new-password")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changePassword(new ChangeOwnPasswordRequest("old", "short")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changePassword(new ChangeOwnPasswordRequest("old", "x".repeat(129))))
                .isInstanceOf(ValidationException.class);
        verify(accounts, never()).saveAndFlush(any());
        verify(passwords, never()).encode(any());
    }

    @Test
    void changesOnlyTheAuthenticatedAccountsPreferredLanguage() {
        Account current = new Account(new Person("Ana"), "ana@example.com", "stored", false);
        Account other = new Account(new Person("Bia"), "bia@example.com", "stored", false);
        when(currentAccountService.require()).thenReturn(current);
        when(accounts.saveAndFlush(current)).thenReturn(current);

        var response = service.updatePreferredLanguage(new UpdateOwnPreferredLanguageRequest("pt-PT"));

        assertThat(response.preferredLanguage()).isEqualTo("pt-PT");
        assertThat(current.getPreferredLanguage()).isEqualTo("pt-PT");
        assertThat(other.getPreferredLanguage()).isEqualTo("en");
        verify(accounts).saveAndFlush(current);
        verifyNoInteractions(persons, passwords);
    }

    @Test
    void rejectsInvalidPreferredLanguagesWithoutPersisting() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "stored", false);
        when(currentAccountService.require()).thenReturn(account);

        for (String language : new String[] {null, "", "pt", "pt-BR", "en-US", "nl-BE"}) {
            assertThatThrownBy(() -> service.updatePreferredLanguage(new UpdateOwnPreferredLanguageRequest(language)))
                    .isInstanceOf(ValidationException.class);
        }
        verify(accounts, never()).saveAndFlush(any());
    }
}
