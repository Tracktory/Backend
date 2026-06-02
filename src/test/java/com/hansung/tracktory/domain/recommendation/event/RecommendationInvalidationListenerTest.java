package com.hansung.tracktory.domain.recommendation.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.hansung.tracktory.domain.profile.event.CompletedSubjectsChangedEvent;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationInvalidationListenerTest {

  private static final long USER_ID = 1L;

  @InjectMocks private RecommendationInvalidationListener listener;

  @Mock private RecommendationRepository recommendationRepository;

  @Test
  void onCompletedSubjectsChanged_supersedesAllActiveRecommendations() {
    Recommendation first = Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    Recommendation second = Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    given(recommendationRepository.findByUser_IdAndStatus(USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(List.of(first, second));

    listener.onCompletedSubjectsChanged(new CompletedSubjectsChangedEvent(USER_ID));

    assertThat(first.getStatus()).isEqualTo(RecommendationStatus.SUPERSEDED);
    assertThat(second.getStatus()).isEqualTo(RecommendationStatus.SUPERSEDED);
  }

  @Test
  void onCompletedSubjectsChanged_noActiveRecommendation_doesNothing() {
    given(recommendationRepository.findByUser_IdAndStatus(USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(List.of());

    listener.onCompletedSubjectsChanged(new CompletedSubjectsChangedEvent(USER_ID));
    // 활성 추천이 없으면 무효화 대상도 없다 — 예외 없이 통과하면 충분.
  }
}
