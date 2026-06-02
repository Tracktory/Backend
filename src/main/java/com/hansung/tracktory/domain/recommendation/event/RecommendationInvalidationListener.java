package com.hansung.tracktory.domain.recommendation.event;

import com.hansung.tracktory.domain.profile.event.CompletedSubjectsChangedEvent;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이수 과목 변경 시 직전에 저장된 추천을 무효화한다.
 *
 * <p>이수 과목을 바꾼 사용자가 추천을 다시 요청하면, 활성 추천이 없으므로 추천이 새로 생성된다(stale 결과 노출 방지). 발행 트랜잭션 안에서 동기로 실행되어 이수
 * 과목 변경과 무효화가 한 단위로 커밋·롤백된다.
 */
@Component
@RequiredArgsConstructor
public class RecommendationInvalidationListener {

  private final RecommendationRepository recommendationRepository;

  @EventListener
  @Transactional
  public void onCompletedSubjectsChanged(CompletedSubjectsChangedEvent event) {
    recommendationRepository
        .findByUser_IdAndStatus(event.userId(), RecommendationStatus.ACTIVE)
        .forEach(Recommendation::markSuperseded);
  }
}
