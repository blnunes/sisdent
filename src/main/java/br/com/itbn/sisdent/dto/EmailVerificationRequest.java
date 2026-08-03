package br.com.itbn.sisdent.dto;

public record EmailVerificationRequest(String token) {
    @Override
    public String toString() {
        return "EmailVerificationRequest[token=[REDACTED]]";
    }
}
