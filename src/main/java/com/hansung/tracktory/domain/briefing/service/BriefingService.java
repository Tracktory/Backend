package com.hansung.tracktory.domain.briefing.service;

import com.hansung.tracktory.domain.briefing.ai.AiBriefingRequest;
import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse;
import com.hansung.tracktory.domain.briefing.ai.BriefingClient;
import com.hansung.tracktory.domain.briefing.dto.BriefingResponse;
import com.hansung.tracktory.domain.briefing.dto.BriefingResponse.BriefingCardView;
import com.hansung.tracktory.domain.briefing.dto.BriefingResponse.SourceView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직무 브리핑 중계 — 사용자의 활성 추천 직무를 기준으로 AI 중계 서버의 큐레이션 브리핑을 조회한다.
 *
 * <p>추천 재계산 없이 홈 추천과 같은 활성 추천 aggregate 를 읽어 직무 코드만 추출하므로 홈 결과와 정합한다. 활성 추천이 없으면 {@code
 * RECOMMENDATION_NOT_FOUND}(404) 로 거절해 새 추천 생성을 유도한다. 브리핑 카드는 AI 서버가 큐레이션 카탈로그에서 직접 채우며, 본 백엔드는 추천
 * 직무 코드 전달과 응답 매핑만 담당한다.
 */
@Service
@RequiredArgsConstructor
public class BriefingService {

  private final RecommendationRepository recommendationRepository;
  private final BriefingClient briefingClient;

  /**
   * 활성 추천 직무 코드로 브리핑 카드를 조회한다. 직무 코드는 적합도(score) 내림차순으로 정렬하고 중복을 제거해 카드 순서가 추천 직무 순서를 따르게 한다.
   *
   * <p>추천 직무가 하나도 없으면 AI 가 빈 {@code job_ids} 를 422 로 거절하므로, 호출하지 않고 빈 결과를 반환한다(부분 충족 허용). 큐레이션이 없는
   * 직무는 AI 응답에서 빠져 카드 수가 추천 직무 수보다 적을 수 있다.
   */
  @Transactional(readOnly = true)
  public BriefingResponse getBriefings(Long userId) {
    Recommendation active =
        recommendationRepository
            .findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, RecommendationStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDATION_NOT_FOUND));

    List<String> jobCodes = jobCodesByScoreDesc(active);
    if (jobCodes.isEmpty()) {
      return new BriefingResponse(List.of());
    }

    AiBriefingResponse aiResponse = briefingClient.fetch(new AiBriefingRequest(jobCodes));
    return toResponse(aiResponse);
  }

  // 추천 직무 코드를 적합도 내림차순 + 등장 순 중복 제거로 추출한다. 같은 코드로 fold 된 중복 직무가 같은 브리핑을 두 번 요청하지 않도록 한다.
  private static List<String> jobCodesByScoreDesc(Recommendation recommendation) {
    Set<String> ordered = new LinkedHashSet<>();
    recommendation.getRecommendedJobs().stream()
        .filter(job -> job.getJob() != null && job.getJob().getCode() != null)
        .sorted(Comparator.comparingInt(RecommendedJob::getScore).reversed())
        .map(job -> job.getJob().getCode())
        .forEach(ordered::add);
    return List.copyOf(ordered);
  }

  private static BriefingResponse toResponse(AiBriefingResponse aiResponse) {
    List<BriefingCardView> cards =
        nullSafe(aiResponse.briefings()).stream().map(BriefingService::toCard).toList();
    return new BriefingResponse(cards);
  }

  private static BriefingCardView toCard(AiBriefingResponse.Briefing briefing) {
    return new BriefingCardView(
        briefing.jobId(),
        briefing.jobName(),
        briefing.headline(),
        briefing.summary(),
        nullSafe(briefing.skills()),
        nullSafe(briefing.sources()).stream().map(BriefingService::toSource).toList());
  }

  private static SourceView toSource(AiBriefingResponse.Source source) {
    return new SourceView(source.title(), source.url(), source.publishedAt());
  }

  private static <T> List<T> nullSafe(List<T> list) {
    return list == null ? List.of() : list;
  }
}
