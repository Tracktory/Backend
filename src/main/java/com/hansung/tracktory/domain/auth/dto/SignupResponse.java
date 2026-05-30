package com.hansung.tracktory.domain.auth.dto;

public record SignupResponse(
        Long userId,
        String email,
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
