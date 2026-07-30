package br.com.itbn.sisdent.service;

public class EmailEnrollmentThrottledException extends RuntimeException {
    private final long retryAfterSeconds;

    public EmailEnrollmentThrottledException(long retryAfterSeconds) {
        super("Please wait before requesting another verification email");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
