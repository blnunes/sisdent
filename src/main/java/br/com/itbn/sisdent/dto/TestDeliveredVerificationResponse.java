package br.com.itbn.sisdent.dto;

public record TestDeliveredVerificationResponse(String token) {
    @Override
    public String toString() {
        return "TestDeliveredVerificationResponse[token=[REDACTED]]";
    }
}
