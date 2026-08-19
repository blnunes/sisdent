package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.CurrentAccountSettingsResponse;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.error.ConflictException;
import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Self-service account settings. Every operation is scoped exclusively to the authenticated account. */
@Service
public class AccountSettingsService {
    private final CurrentAccountService currentAccountService;
    private final PersonRepository persons;
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;

    public AccountSettingsService(CurrentAccountService currentAccountService, PersonRepository persons,
            AccountRepository accounts, PasswordEncoder passwords) {
        this.currentAccountService = currentAccountService;
        this.persons = persons;
        this.accounts = accounts;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public CurrentAccountSettingsResponse current() {
        return response(currentAccountService.require());
    }

    @Transactional
    public CurrentAccountSettingsResponse updateProfile(UpdateOwnProfileRequest request) {
        Account account = currentAccountService.require();
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        if (displayName.isEmpty()) throw new ValidationException(ErrorCode.ACCOUNT_DISPLAY_NAME_INVALID);
        Person person = account.getPerson();
        if (person.getVersion() != request.version()) {
            throw new ConflictException(ErrorCode.ACCOUNT_PROFILE_VERSION_CONFLICT);
        }
        person.changeDisplayName(displayName);
        persons.saveAndFlush(person);
        return response(account);
    }

    @Transactional
    public void changePassword(ChangeOwnPasswordRequest request) {
        Account account = currentAccountService.require();
        validatePasswordRequest(request);
        if (!passwords.matches(request.currentPassword(), account.getPassword())) {
            throw new ValidationException(ErrorCode.ACCOUNT_CURRENT_PASSWORD_INVALID);
        }
        account.changePassword(passwords.encode(request.newPassword()));
        accounts.saveAndFlush(account);
    }

    private static void validatePasswordRequest(ChangeOwnPasswordRequest request) {
        if (request.currentPassword() == null || request.currentPassword().isBlank()
                || request.newPassword() == null || request.newPassword().length() < 8
                || request.newPassword().length() > 128) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static CurrentAccountSettingsResponse response(Account account) {
        Person person = account.getPerson();
        return new CurrentAccountSettingsResponse(account.getGlobalId(), person.getDisplayName(), account.getEmail(), person.getVersion());
    }
}
