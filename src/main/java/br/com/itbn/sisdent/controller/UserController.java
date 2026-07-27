package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.PermissionRequest;
import br.com.itbn.sisdent.dto.PasswordChangeRequest;
import br.com.itbn.sisdent.dto.PasswordChangeResponse;
import br.com.itbn.sisdent.dto.UserRequest;
import br.com.itbn.sisdent.dto.UserResponse;
import br.com.itbn.sisdent.dto.UserUpdateRequest;
import br.com.itbn.sisdent.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PatchMapping("/me/password")
    public PasswordChangeResponse changeOwnPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequest request) {
        Number userId = jwt.getClaim("userId");
        userService.changeOwnPassword(userId.longValue(), request);
        return new PasswordChangeResponse("Password changed successfully");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @PutMapping("/{id}/permissions")
    public UserResponse updatePermissions(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequest request) {
        return userService.updatePermissions(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
