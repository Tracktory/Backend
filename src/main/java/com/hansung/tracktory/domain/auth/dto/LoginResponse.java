package com.hansung.tracktory.domain.auth.dto;

public record LoginResponse(
        Long userId,
        String email,
        String accessToken,
        String tokenType,
        long expiresIn,
        boolean onboardingCompleted
) {
}
