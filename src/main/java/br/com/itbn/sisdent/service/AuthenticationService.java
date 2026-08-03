package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.LoginRequest;
import br.com.itbn.sisdent.dto.TokenResponse;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.repository.AccountRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse authenticate(LoginRequest request) {
        Account account = accountRepository.findByEmail(Account.normalizeEmail(request.email()))
                .filter(Account::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return jwtService.issue(account);
    }
}
