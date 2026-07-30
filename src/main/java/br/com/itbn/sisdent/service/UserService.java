package br.com.itbn.sisdent.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;

import br.com.itbn.sisdent.dto.PasswordChangeRequest;
import br.com.itbn.sisdent.dto.PermissionRequest;
import br.com.itbn.sisdent.dto.UserRequest;
import br.com.itbn.sisdent.dto.UserResponse;
import br.com.itbn.sisdent.dto.UserUpdateRequest;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.pagination.PageQuery;
import br.com.itbn.sisdent.pagination.PageableFactory;
import br.com.itbn.sisdent.pagination.SortDefinition;
import br.com.itbn.sisdent.model.Role;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.UserRepository;

@Service
public class UserService {
    private static final SortDefinition SORT_DEFINITION = new SortDefinition("identificationNumber", java.util.Set.of("id", "identificationNumber", "role"));

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PageableFactory pageableFactory;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PageableFactory pageableFactory) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pageableFactory = pageableFactory;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAllByActiveTrueOrderByIdentificationNumber().stream()
                .map(UserService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findPage(PageQuery query) {
        return PageResponse.from(userRepository.findAllByActiveTrue(pageableFactory.create(query, SORT_DEFINITION)), UserService::toResponse);
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
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é possível alterar permissões do ADMIN");
        }
        user.setPermissions(request.permissions());
        return toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void delete(Long id) {
        User user = requireActive(id);
        user.deactivate();
        userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(Long userId, PasswordChangeRequest request) {
        User user = requireActive(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCurrentPasswordException();
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
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
