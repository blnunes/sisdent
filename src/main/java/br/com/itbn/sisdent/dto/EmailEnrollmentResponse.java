package br.com.itbn.sisdent.dto;

public record EmailEnrollmentResponse(String status) {
    public static EmailEnrollmentResponse challengeSent() {
        return new EmailEnrollmentResponse("CHALLENGE_SENT");
    }
}
