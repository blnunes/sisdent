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

import java.io.IOException;
import java.util.UUID;

@Component
public class AccountStateJwtFilter extends OncePerRequestFilter {
    private final AccountRepository accountRepository;

    public AccountStateJwtFilter(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication()
                instanceof JwtAuthenticationToken authentication) {
            boolean tokenMigrationRequired = Boolean.TRUE.equals(
                    authentication.getToken().getClaim("emailMigrationRequired"));
            boolean current = findCurrentState(authentication.getName())
                    .map(state -> state.active()
                            && state.emailMigrationRequired() == tokenMigrationRequired)
                    .orElse(false);
            if (!current) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<AccountState> findCurrentState(String subject) {
        try {
            return accountRepository.findByGlobalId(UUID.fromString(subject))
                    .map(account -> new AccountState(
                            account.isActive(), account.isEmailMigrationRequired()));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private record AccountState(boolean active, boolean emailMigrationRequired) {
    }
}
