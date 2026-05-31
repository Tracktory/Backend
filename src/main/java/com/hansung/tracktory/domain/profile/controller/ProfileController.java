package com.hansung.tracktory.domain.profile.controller;

import com.hansung.tracktory.domain.profile.dto.ProfileResponse;
import com.hansung.tracktory.domain.profile.service.ProfileQueryService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/profile")
@RequiredArgsConstructor
public class ProfileController {

  private final ProfileQueryService profileQueryService;

  @GetMapping
  public ApiResponse<ProfileResponse> getMyProfile(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResponse.ok(profileQueryService.getMyProfile(principal.getUserId()));
  }
}
