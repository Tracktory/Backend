package com.hansung.tracktory.domain.recommendation.service;

import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileReader;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상세 분석 리포트 조회 — 사용자의 활성 추천을 기준으로 읽기 전용 리포트를 조립한다.
 *
 * <p>AI 재호출이나 추천 재계산 없이 홈 추천과 같은 활성 추천 aggregate 를 읽어 조립하므로 홈 결과와 정합한다. 활성 추천이 없으면(이수 과목 변경으로 직전
 * 추천이 무효화된 경우 포함) {@code RECOMMENDATION_NOT_FOUND}(404) 로 거절해 새 추천 생성을 유도한다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationReportService {

  private final OnboardingProfileReader onboardingProfileReader;
  private final RecommendationRepository recommendationRepository;
  private final AnalysisReportAssembler analysisReportAssembler;

  /**
   * 활성 추천을 기준으로 리포트를 조립한다. {@code anchorJobCode} 가 주어지면 그 직무를 충족도 기준으로 하고, 없으면 매칭 1순위 직무가 기준이 된다(추천
   * 직무 목록에 없는 코드는 assembler 에서 {@code INVALID_ANCHOR_JOB} 로 거절).
   */
  @Transactional(readOnly = true)
  public AnalysisReportResponse getReport(Long userId, String anchorJobCode) {
    OnboardingProfileSnapshot profile =
        onboardingProfileReader
            .read(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
    Recommendation active =
        recommendationRepository
            .findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, RecommendationStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));
    return analysisReportAssembler.assemble(active, profile, anchorJobCode);
  }
}
