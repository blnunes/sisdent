package br.com.itbn.sisdent.service;

public interface EmailVerificationDelivery {
    String providerName();

    String deliver(Long accountId, String targetEmail, String secret);
}
