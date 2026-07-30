package br.com.itbn.sisdent.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailEnrollmentRequest(
        @NotBlank @Size(max = 320) String email) {

    @AssertTrue(message = "email must be valid")
    public boolean isEmailValid() {
        if (email == null) {
            return false;
        }
        String normalized = email.strip();
        int separator = normalized.lastIndexOf('@');
        return separator > 0
                && separator < normalized.length() - 1
                && normalized.chars().noneMatch(Character::isWhitespace)
                && normalized.indexOf('@') == separator;
    }
}
