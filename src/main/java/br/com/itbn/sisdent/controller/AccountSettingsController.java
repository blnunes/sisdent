package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.ChangeOwnPasswordRequest;
import br.com.itbn.sisdent.dto.CurrentAccountSettingsResponse;
import br.com.itbn.sisdent.dto.UpdateOwnProfileRequest;
import br.com.itbn.sisdent.dto.UpdateOwnPreferredLanguageRequest;
import br.com.itbn.sisdent.service.AccountSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/account/settings")
public class AccountSettingsController {
    private final AccountSettingsService settings;

    public AccountSettingsController(AccountSettingsService settings) { this.settings = settings; }

    @GetMapping
    public CurrentAccountSettingsResponse current() { return settings.current(); }

    @PatchMapping("/profile")
    public CurrentAccountSettingsResponse updateProfile(@Valid @RequestBody UpdateOwnProfileRequest request) {
        return settings.updateProfile(request);
    }

    @PatchMapping("/preferred-language")
    public CurrentAccountSettingsResponse updatePreferredLanguage(@RequestBody UpdateOwnPreferredLanguageRequest request) {
        return settings.updatePreferredLanguage(request);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangeOwnPasswordRequest request) {
        settings.changePassword(request);
    }

    @PutMapping(value = "/avatar", consumes = "multipart/form-data")
    public CurrentAccountSettingsResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        return settings.uploadAvatar(file);
    }

    @DeleteMapping("/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAvatar() { settings.removeAvatar(); }

    @GetMapping("/avatar")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> avatar() {
        var avatar = settings.avatar();
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(avatar.contentType()))
                .contentLength(avatar.length()).cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(new org.springframework.core.io.InputStreamResource(avatar.content()));
    }
}
