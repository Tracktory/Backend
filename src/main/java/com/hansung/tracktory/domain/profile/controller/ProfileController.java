package com.hansung.tracktory.domain.profile.controller;

import com.hansung.tracktory.domain.profile.dto.ProfileResponse;
import com.hansung.tracktory.domain.profile.dto.ProfileUpdateRequest;
import com.hansung.tracktory.domain.profile.dto.ProfileUpdateResponse;
import com.hansung.tracktory.domain.profile.service.ProfileQueryService;
import com.hansung.tracktory.domain.profile.service.ProfileUpdateService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileQueryService profileQueryService;
  private final ProfileUpdateService profileUpdateService;

  @GetMapping
  public ApiResponse<ProfileResponse> getMyProfile(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(profileQueryService.getMyProfile(principal.getUserId()));
  }

  @PatchMapping
  public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ProfileUpdateRequest request) {
    ProfileUpdateResponse response = profileUpdateService.update(principal.getUserId(), request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
