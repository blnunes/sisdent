package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CurrentAccountService {
    private final AccountRepository accountRepository;

    public CurrentAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account require() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            return accountRepository.findByGlobalId(UUID.fromString(subject))
                    .filter(Account::isActive)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account identity", exception);
        }
    }
}
