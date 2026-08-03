package br.com.itbn.sisdent.service;

public class EmailEnrollmentUnavailableException extends RuntimeException {
    public EmailEnrollmentUnavailableException() {
        super("Email enrollment could not be started");
    }
}
