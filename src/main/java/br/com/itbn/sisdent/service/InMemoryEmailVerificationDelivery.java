package br.com.itbn.sisdent.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"test", "e2e", "development"})
public class InMemoryEmailVerificationDelivery implements EmailVerificationDelivery {
    private final Map<Long, DeliveredVerification> deliveries = new ConcurrentHashMap<>();

    @Override
    public String providerName() {
        return "in-memory";
    }

    @Override
    public String deliver(Long accountId, String targetEmail, String secret) {
        String messageId = UUID.randomUUID().toString();
        deliveries.put(accountId, new DeliveredVerification(targetEmail, secret, messageId));
        return messageId;
    }

    public Optional<DeliveredVerification> latestFor(Long accountId) {
        return Optional.ofNullable(deliveries.get(accountId));
    }

    public record DeliveredVerification(String targetEmail, String secret, String messageId) {
        @Override
        public String toString() {
            return "DeliveredVerification[targetEmail=" + targetEmail
                    + ", secret=[REDACTED], messageId=" + messageId + "]";
        }
    }
}
