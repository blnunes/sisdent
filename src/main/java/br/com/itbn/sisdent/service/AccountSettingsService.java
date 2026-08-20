package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.CurrentAccountSettingsResponse;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.dto.UpdateOwnPreferredLanguageRequest;
import br.com.itbn.sisdent.avatar.ProfileAvatarProcessor;
import br.com.itbn.sisdent.avatar.ProfileAvatarStorage;
import br.com.itbn.sisdent.error.ResourceNotFoundException;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.io.InputStream;
import java.util.UUID;

/** Self-service account settings. Every operation is scoped exclusively to the authenticated account. */
@Service
public class AccountSettingsService {
    private final CurrentAccountService currentAccountService;
    private final PersonRepository persons;
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final ProfileAvatarStorage avatarStorage;
    private final ProfileAvatarProcessor avatarProcessor = new ProfileAvatarProcessor();

    public AccountSettingsService(CurrentAccountService currentAccountService, PersonRepository persons,
            AccountRepository accounts, PasswordEncoder passwords, ProfileAvatarStorage avatarStorage) {
        this.currentAccountService = currentAccountService;
        this.persons = persons;
        this.accounts = accounts;
        this.passwords = passwords;
        this.avatarStorage = avatarStorage;
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

    @Transactional
    public CurrentAccountSettingsResponse updatePreferredLanguage(UpdateOwnPreferredLanguageRequest request) {
        Account account = currentAccountService.require();
        try {
            account.changePreferredLanguage(request == null ? null : request.preferredLanguage());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(ErrorCode.ACCOUNT_PREFERRED_LANGUAGE_INVALID);
        }
        accounts.saveAndFlush(account);
        return response(account);
    }

    @Transactional
    public CurrentAccountSettingsResponse uploadAvatar(MultipartFile file) {
        Account account = currentAccountService.require();
        ProfileAvatarProcessor.ProcessedAvatar processed = avatarProcessor.process(file);
        String newKey = avatarKey(account);
        avatarStorage.save(newKey, processed.content());
        String oldKey = account.getAvatarKey();
        String oldContentType = account.getAvatarContentType();
        Instant oldUpdatedAt = account.getAvatarUpdatedAt();
        try {
            account.replaceAvatar(newKey, processed.contentType(), Instant.now());
            accounts.saveAndFlush(account);
        } catch (RuntimeException exception) {
            if (oldKey == null) account.removeAvatar();
            else account.replaceAvatar(oldKey, oldContentType, oldUpdatedAt);
            safeDelete(newKey);
            throw exception;
        }
        if (oldKey != null) afterCommit(() -> safeDelete(oldKey));
        return response(account);
    }

    @Transactional
    public void removeAvatar() {
        Account account = currentAccountService.require();
        String oldKey = account.getAvatarKey();
        if (oldKey == null) return;
        account.removeAvatar();
        accounts.saveAndFlush(account);
        afterCommit(() -> safeDelete(oldKey));
    }

    @Transactional(readOnly = true)
    public AvatarContent avatar() {
        Account account = currentAccountService.require();
        if (!account.hasAvatar()) throw new ResourceNotFoundException(ErrorCode.ACCOUNT_AVATAR_NOT_FOUND);
        ProfileAvatarStorage.StoredProfileAvatar stored = avatarStorage.get(account.getAvatarKey());
        return new AvatarContent(stored.content(), stored.length(), account.getAvatarContentType());
    }

    private static String avatarKey(Account account) {
        return "account_" + account.getGlobalId().toString().replace("-", "") + "_" + UUID.randomUUID().toString().replace("-", "") + ".png";
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    private void safeDelete(String key) {
        try { avatarStorage.delete(key); } catch (RuntimeException ignored) { /* retained for a later cleanup attempt */ }
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
        return new CurrentAccountSettingsResponse(account.getGlobalId(), person.getDisplayName(), account.getEmail(),
                account.getPreferredLanguage(), AvatarUrls.forAccount(account), person.getVersion());
    }

    public record AvatarContent(InputStream content, long length, String contentType) { }
}
