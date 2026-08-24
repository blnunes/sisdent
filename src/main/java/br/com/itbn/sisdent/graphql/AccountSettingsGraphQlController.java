package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.CurrentAccountSettingsResponse;
import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.UpdateOwnPreferredLanguageRequest;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.service.AccountSettingsService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Base64;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Transport-only adapter for the authenticated account-settings workflow. */
@Controller
public class AccountSettingsGraphQlController {
    private final AccountSettingsService settings;

    public AccountSettingsGraphQlController(AccountSettingsService settings) {
        this.settings = settings;
    }

    @QueryMapping
    public CurrentAccountSettingsResponse currentAccountSettings() {
        return settings.current();
    }

    @MutationMapping
    public CurrentAccountSettingsResponse updateOwnProfile(@Argument @Valid UpdateOwnProfileRequest input) {
        return settings.updateProfile(input);
    }

    @MutationMapping
    public CurrentAccountSettingsResponse updateOwnPreferredLanguage(
            @Argument UpdateOwnPreferredLanguageRequest input) {
        return settings.updatePreferredLanguage(input);
    }

    @MutationMapping
    public boolean changeOwnPassword(@Argument @Valid ChangeOwnPasswordRequest input) {
        settings.changePassword(input);
        return true;
    }

    @MutationMapping
    public CurrentAccountSettingsResponse uploadOwnAvatar(@Argument AvatarUploadInput input) {
        return settings.uploadAvatar(input.toMultipartFile());
    }

    @MutationMapping
    public boolean removeOwnAvatar() {
        settings.removeAvatar();
        return true;
    }

    @QueryMapping
    public AvatarDownload ownAvatar() throws IOException {
        AccountSettingsService.AvatarContent avatar = settings.avatar();
        return new AvatarDownload(avatar.contentType(), Base64.getEncoder().encodeToString(avatar.content().readAllBytes()));
    }

    public record AvatarDownload(String contentType, String contentBase64) { }
}
