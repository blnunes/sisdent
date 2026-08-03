package br.com.itbn.sisdent.service;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException() {
        super("Email enrollment is temporarily unavailable");
    }
}
