package br.com.itbn.sisdent.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
