package com.hansung.tracktory.domain.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.briefing.ai.AiBriefingRequest;
import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse;
import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse.Briefing;
import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse.Source;
import com.hansung.tracktory.domain.briefing.ai.BriefingClient;
import com.hansung.tracktory.domain.briefing.dto.BriefingResponse;
import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationTriggerSource;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

  private static final long USER_ID = 1L;

  @InjectMocks private BriefingService service;

  @Mock private RecommendationRepository recommendationRepository;
  @Mock private BriefingClient briefingClient;

  @Test
  void getBriefings_noActiveRecommendation_throwsRecommendationNotFound() {
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> service.getBriefings(USER_ID))
        .isInstanceOf(BusinessException.class)
        .extracting(e -> ((BusinessException) e).getErrorCode())
        .isEqualTo(ErrorCode.RECOMMENDATION_NOT_FOUND);

    verify(briefingClient, never()).fetch(any());
  }

  @Test
  void getBriefings_noRecommendedJobs_returnsEmptyWithoutCallingClient() {
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.of(activeRecommendation()));

    BriefingResponse response = service.getBriefings(USER_ID);

    assertThat(response.briefings()).isEmpty();
    verify(briefingClient, never()).fetch(any());
  }

  @Test
  void getBriefings_passesJobCodesByScoreDescDedup_andMapsResponse() {
    Recommendation recommendation = activeRecommendation();
    recommendation.addRecommendedJob(recommendedJob("BE", "백엔드 개발자", 70));
    recommendation.addRecommendedJob(recommendedJob("AI", "AI/ML 엔지니어", 90));
    recommendation.addRecommendedJob(recommendedJob("FE", "프론트엔드 개발자", 80));
    recommendation.addRecommendedJob(recommendedJob("AI", "AI/ML 엔지니어", 60)); // 같은 코드 중복
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.of(recommendation));

    given(briefingClient.fetch(any()))
        .willReturn(
            new AiBriefingResponse(
                List.of(
                    new Briefing(
                        "AI",
                        "AI/ML 엔지니어",
                        "LLM 응용 수요 급증",
                        "RAG·파인튜닝 역량이 핵심으로 떠오른다.",
                        List.of("Python", "PyTorch"),
                        List.of(new Source("AI Index Report", "https://aiindex.org", "2024"))))));

    BriefingResponse response = service.getBriefings(USER_ID);

    // 적합도 내림차순 + 등장 순 중복 제거: AI(90) > FE(80) > BE(70), 중복 AI 는 한 번만.
    ArgumentCaptor<AiBriefingRequest> captor = ArgumentCaptor.forClass(AiBriefingRequest.class);
    verify(briefingClient).fetch(captor.capture());
    assertThat(captor.getValue().jobIds()).containsExactly("AI", "FE", "BE");

    assertThat(response.briefings()).hasSize(1);
    BriefingResponse.BriefingCardView card = response.briefings().get(0);
    assertThat(card.code()).isEqualTo("AI");
    assertThat(card.name()).isEqualTo("AI/ML 엔지니어");
    assertThat(card.headline()).isEqualTo("LLM 응용 수요 급증");
    assertThat(card.skills()).containsExactly("Python", "PyTorch");
    assertThat(card.sources()).hasSize(1);
    assertThat(card.sources().get(0).title()).isEqualTo("AI Index Report");
    assertThat(card.sources().get(0).url()).isEqualTo("https://aiindex.org");
    assertThat(card.sources().get(0).publishedAt()).isEqualTo("2024");
  }

  // ------------------------------ helpers ------------------------------

  private static Recommendation activeRecommendation() {
    return Recommendation.builder()
        .status(RecommendationStatus.ACTIVE)
        .triggerSource(RecommendationTriggerSource.MANUAL)
        .build();
  }

  private static RecommendedJob recommendedJob(String code, String name, int score) {
    Job job = Job.builder().code(code).name(name).description("설명").build();
    return RecommendedJob.builder()
        .score(score)
        .reasoning("근거")
        .competencyTags(List.of())
        .job(job)
        .build();
  }
}
