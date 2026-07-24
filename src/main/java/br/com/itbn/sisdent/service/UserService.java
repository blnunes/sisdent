package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PermissionRequest;
import br.com.itbn.sisdent.dto.UserRequest;
import br.com.itbn.sisdent.dto.UserResponse;
import br.com.itbn.sisdent.dto.UserUpdateRequest;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByActiveTrueOrderByIdentificationNumber().stream()
                .map(UserService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(requireActive(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        User user = new User(
                request.identificationType(),
                IdentificationNumbers.normalize(request.identificationNumber()),
                passwordEncoder.encode(request.password()),
                request.role(),
                request.role().defaultPermissions());
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = requireActive(id);
        String encodedPassword = request.password() == null || request.password().isBlank()
                ? null
                : passwordEncoder.encode(request.password());
        user.update(
                request.identificationType(),
                IdentificationNumbers.normalize(request.identificationNumber()),
                encodedPassword,
                request.role());
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse updatePermissions(Long id, PermissionRequest request) {
        User user = requireActive(id);
        user.setPermissions(request.permissions());
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = requireActive(id);
        user.deactivate();
        userRepository.save(user);
    }

    private User requireActive(Long id) {
        return userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getIdentificationType(),
                user.getIdentificationNumber(),
                user.getRole(),
                user.getPermissions(),
                user.isActive());
    }
}
