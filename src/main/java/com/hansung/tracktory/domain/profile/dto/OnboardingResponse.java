package com.hansung.tracktory.domain.profile.dto;

public record OnboardingResponse(boolean onboardingCompleted) {

  public static OnboardingResponse completed() {
    return new OnboardingResponse(true);
  }
}
