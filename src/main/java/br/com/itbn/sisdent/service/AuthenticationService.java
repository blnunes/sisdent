package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.LoginRequest;
import br.com.itbn.sisdent.dto.TokenResponse;
import br.com.itbn.sisdent.model.User;
import br.com.itbn.sisdent.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse authenticate(LoginRequest request) {
        User user = userRepository.findByIdentificationTypeAndIdentificationNumber(
                        request.identificationType(),
                        IdentificationNumbers.normalize(request.identificationNumber()))
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return jwtService.issue(user);
    }

}
