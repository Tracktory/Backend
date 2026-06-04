package com.hansung.tracktory.domain.recommendation.controller;

import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.service.RecommendationReportService;
import com.hansung.tracktory.domain.recommendation.service.RecommendationService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 생성·조회 API — 인증된 사용자의 온보딩 데이터로 직무·트랙·로드맵 추천을 생성하고, 활성 추천 기준 상세 분석 리포트를 조회한다.
 *
 * <p>온보딩 미완료 사용자는 {@code ONBOARDING_NOT_FOUND}(404) 로 거절된다. {@code forceRefresh=true} 면 기존 활성 추천을
 * 무시하고 새로 생성한다.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final RecommendationReportService recommendationReportService;

  @PostMapping
  public ResponseEntity<ApiResponse<RecommendationResponse>> generate(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "forceRefresh", defaultValue = "false") boolean forceRefresh) {
    RecommendationResponse response =
        recommendationService.generate(principal.getUserId(), forceRefresh);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  /**
   * 상세 분석 리포트 조회 — 활성 추천을 기준으로 기준 직무·역량 충족도·분야별 분석·잔여 과목·다음 액션·집계 수치를 반환한다.
   *
   * <p>{@code anchorJobCode} 로 충족도 기준 직무를 지정할 수 있다. 생략하면 매칭 1순위 직무가 기준이 되고, 추천 직무 목록에 없는 코드는 {@code
   * INVALID_ANCHOR_JOB}(400) 로 거절된다. 활성 추천이 없으면 {@code RECOMMENDATION_NOT_FOUND}(404) 로 거절된다.
   */
  @GetMapping("/report")
  public ResponseEntity<ApiResponse<AnalysisReportResponse>> getReport(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(name = "anchorJobCode", required = false) String anchorJobCode) {
    AnalysisReportResponse response =
        recommendationReportService.getReport(principal.getUserId(), anchorJobCode);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
