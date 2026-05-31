package com.hansung.tracktory.domain.profile.controller;

import com.hansung.tracktory.domain.profile.dto.OnboardingRequest;
import com.hansung.tracktory.domain.profile.dto.OnboardingResponse;
import com.hansung.tracktory.domain.profile.service.OnboardingService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  private final OnboardingService onboardingService;

  @PostMapping
  public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody OnboardingRequest request) {
    OnboardingResponse response = onboardingService.onboard(principal.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }
}
