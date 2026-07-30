package br.com.itbn.sisdent.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test & !e2e & !development")
public class UnavailableEmailVerificationDelivery implements EmailVerificationDelivery {

    @Override
    public String providerName() {
        return "unavailable";
    }

    @Override
    public String deliver(Long accountId, String targetEmail, String secret) {
        throw new EmailDeliveryUnavailableException();
    }
}
