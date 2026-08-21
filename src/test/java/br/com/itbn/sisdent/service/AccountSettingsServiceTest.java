package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.dto.UpdateOwnPreferredLanguageRequest;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.avatar.ProfileAvatarStorage;
import br.com.itbn.sisdent.avatar.ProfileAvatarProcessor;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import br.com.itbn.sisdent.error.ErrorCode;

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
    @Mock ProfileAvatarStorage avatarStorage;
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
        UpdateOwnProfileRequest request = new UpdateOwnProfileRequest("  ", 0L);
        assertThatThrownBy(() -> service.updateProfile(request))
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

        ChangeOwnPasswordRequest invalidCurrentPassword = new ChangeOwnPasswordRequest("wrong", "new-password");
        ChangeOwnPasswordRequest shortNewPassword = new ChangeOwnPasswordRequest("old", "short");
        ChangeOwnPasswordRequest oversizedNewPassword = new ChangeOwnPasswordRequest("old", "x".repeat(129));
        assertThatThrownBy(() -> service.changePassword(invalidCurrentPassword))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changePassword(shortNewPassword))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.changePassword(oversizedNewPassword))
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
            UpdateOwnPreferredLanguageRequest request = new UpdateOwnPreferredLanguageRequest(language);
            assertThatThrownBy(() -> service.updatePreferredLanguage(request))
                    .isInstanceOf(ValidationException.class);
        }
        verify(accounts, never()).saveAndFlush(any());
    }

    @Test
    void storesOnlyAProcessedAvatarForTheAuthenticatedAccountAndRemovesThePreviousOne() throws java.io.IOException {
        Account account = new Account(new Person("Ana"), "ana@example.com", "stored", false);
        account.replaceAvatar("account_old.png", "image/png", java.time.Instant.now());
        when(currentAccountService.require()).thenReturn(account);
        when(accounts.saveAndFlush(account)).thenReturn(account);

        var response = service.uploadAvatar(image("image/png", "photo.png"));

        assertThat(response.avatarUrl()).contains("/api/account/settings/avatar?v=");
        assertThat(account.getAvatarKey()).matches("account_[a-f0-9]{32}_[a-f0-9]{32}\\.png");
        verify(avatarStorage).save(matches("account_[a-f0-9]{32}_[a-f0-9]{32}\\.png"), argThat(bytes -> bytes.length > 0));
        verify(accounts).saveAndFlush(account);
    }

    @Test
    void deletesNewAvatarWhenPersistenceFailsAndRemovalIsIdempotent() throws java.io.IOException {
        Account account = new Account(new Person("Ana"), "ana@example.com", "stored", false);
        when(currentAccountService.require()).thenReturn(account);
        doThrow(new RuntimeException("database unavailable")).when(accounts).saveAndFlush(account);

        MockMultipartFile avatar = image("image/png", "photo.png");
        assertThatThrownBy(() -> service.uploadAvatar(avatar)).isInstanceOf(RuntimeException.class);
        verify(avatarStorage).delete(matches("account_[a-f0-9]{32}_[a-f0-9]{32}\\.png"));

        reset(accounts, avatarStorage);
        service.removeAvatar();
        verifyNoInteractions(accounts, avatarStorage);
    }

    @Test
    void rejectsEmptyAndDisguisedAvatarContent() {
        when(currentAccountService.require()).thenReturn(new Account(new Person("Ana"), "ana@example.com", "stored", false));
        MockMultipartFile emptyAvatar = new MockMultipartFile("file", "photo.png", "image/png", new byte[0]);
        MockMultipartFile disguisedAvatar = new MockMultipartFile("file", "photo.png", "image/png", "not image".getBytes());
        assertThatThrownBy(() -> service.uploadAvatar(emptyAvatar))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.uploadAvatar(disguisedAvatar))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(avatarStorage, accounts);
    }

    @Test
    void acceptsRealPngAndJpegButRejectsUnsupportedWebpAndExcessiveDimensions() throws java.io.IOException {
        when(currentAccountService.require()).thenReturn(new Account(new Person("Ana"), "ana@example.com", "stored", false));
        when(accounts.saveAndFlush(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.uploadAvatar(image("image/png", "photo.png"));
        service.uploadAvatar(image("image/jpeg", "photo.jpeg"));
        MockMultipartFile webpAvatar = new MockMultipartFile("file", "photo.webp", "image/webp", webpHeader());
        ValidationException webp = org.junit.jupiter.api.Assertions.assertThrows(ValidationException.class,
                () -> service.uploadAvatar(webpAvatar));
        assertThat(webp.errorCode()).isEqualTo(ErrorCode.ACCOUNT_AVATAR_INVALID_TYPE);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        MockMultipartFile wideAvatar = new MockMultipartFile("file", "wide.png", "image/png", bytes.toByteArray());
        assertThatThrownBy(() -> service.uploadAvatar(wideAvatar))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsFilesOverFiveMiBBeforeStorage() {
        when(currentAccountService.require()).thenReturn(new Account(new Person("Ana"), "ana@example.com", "stored", false));
        MockMultipartFile oversizedAvatar = new MockMultipartFile("file", "large.png", "image/png",
                new byte[(int) ProfileAvatarProcessor.MAX_UPLOAD_BYTES + 1]);
        assertThatThrownBy(() -> service.uploadAvatar(oversizedAvatar))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(avatarStorage, accounts);
    }

    private static MockMultipartFile image(String contentType, String name) throws java.io.IOException {
        BufferedImage image = new BufferedImage(800, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, contentType.equals("image/jpeg") ? "jpeg" : "png", bytes);
        return new MockMultipartFile("file", name, contentType, bytes.toByteArray());
    }

    private static byte[] webpHeader() {
        return Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/vuUAAA=");
    }
}
