package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.TestDeliveredVerificationResponse;
import br.com.itbn.sisdent.service.CurrentAccountService;
import br.com.itbn.sisdent.service.InMemoryEmailVerificationDelivery;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("e2e")
@RequestMapping("/api/test-support/email-verifications")
public class EmailVerificationTestSupportController {
    private final CurrentAccountService currentAccountService;
    private final InMemoryEmailVerificationDelivery delivery;

    public EmailVerificationTestSupportController(
            CurrentAccountService currentAccountService,
            InMemoryEmailVerificationDelivery delivery) {
        this.currentAccountService = currentAccountService;
        this.delivery = delivery;
    }

    @GetMapping("/latest")
    public TestDeliveredVerificationResponse latest() {
        Long accountId = currentAccountService.require().getId();
        return delivery.latestFor(accountId)
                .map(value -> new TestDeliveredVerificationResponse(value.secret()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
