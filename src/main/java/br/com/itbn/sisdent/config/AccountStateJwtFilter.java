package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import br.com.itbn.sisdent.controller.RestExceptionTranslator;

import java.io.IOException;
import java.util.UUID;

@Component
public class AccountStateJwtFilter extends OncePerRequestFilter {
    private final AccountRepository accountRepository;
    private final RestExceptionTranslator exceptionTranslator;

    public AccountStateJwtFilter(AccountRepository accountRepository, RestExceptionTranslator exceptionTranslator) {
        this.accountRepository = accountRepository;
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken authentication) {
            boolean current = findCurrentState(authentication.getName())
                    .map(AccountState::active)
                    .orElse(false);
            if (!current) {
                SecurityContextHolder.clearContext();
                exceptionTranslator.authentication(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<AccountState> findCurrentState(String subject) {
        try {
            return accountRepository.findByGlobalId(UUID.fromString(subject))
                    .map(account -> new AccountState(account.isActive()));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private record AccountState(boolean active) {
    }
}
