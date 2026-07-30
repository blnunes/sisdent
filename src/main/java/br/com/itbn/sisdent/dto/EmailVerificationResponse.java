package br.com.itbn.sisdent.dto;

public record EmailVerificationResponse(String status) {
    public static EmailVerificationResponse verified() {
        return new EmailVerificationResponse("VERIFIED");
    }

    public static EmailVerificationResponse invalidOrExpired() {
        return new EmailVerificationResponse("INVALID_OR_EXPIRED");
    }
}
