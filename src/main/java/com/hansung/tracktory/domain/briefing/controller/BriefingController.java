package com.hansung.tracktory.domain.briefing.controller;

import com.hansung.tracktory.domain.briefing.dto.BriefingResponse;
import com.hansung.tracktory.domain.briefing.service.BriefingService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 직무 브리핑 조회 API — 인증된 사용자의 활성 추천 직무에 연결된 트렌드 브리핑 카드를 반환한다.
 *
 * <p>앱은 AI 서버를 직접 호출하지 않고 본 백엔드를 경유한다. 활성 추천이 없으면 {@code RECOMMENDATION_NOT_FOUND}(404) 로 거절된다.
 */
@RestController
@RequestMapping("/api/v1/briefings")
@RequiredArgsConstructor
public class BriefingController {

  private final BriefingService briefingService;

  @GetMapping
  public ResponseEntity<ApiResponse<BriefingResponse>> getBriefings(
      @AuthenticationPrincipal UserPrincipal principal) {
    BriefingResponse response = briefingService.getBriefings(principal.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
