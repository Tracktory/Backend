package com.hansung.tracktory.domain.recommendation.controller;

import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.service.RecommendationService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 생성 API — 인증된 사용자의 온보딩 데이터로 직무·트랙·로드맵 추천을 한 번에 생성한다.
 *
 * <p>온보딩 미완료 사용자는 {@code ONBOARDING_NOT_FOUND}(404) 로 거절된다. {@code forceRefresh=true} 면 기존 활성 추천을
 * 무시하고 새로 생성한다.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;

  @PostMapping
  public ResponseEntity<ApiResponse<RecommendationResponse>> generate(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "forceRefresh", defaultValue = "false") boolean forceRefresh) {
    RecommendationResponse response =
        recommendationService.generate(principal.getUserId(), forceRefresh);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
