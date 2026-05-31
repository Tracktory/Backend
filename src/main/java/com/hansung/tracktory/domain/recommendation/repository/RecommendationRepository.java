package com.hansung.tracktory.domain.recommendation.repository;

import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 추천 aggregate 영속성. 자식(직무/트랙/로드맵)은 cascade 로 함께 저장·조회된다. */
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

  List<Recommendation> findByUser_IdAndStatus(Long userId, RecommendationStatus status);

  Optional<Recommendation> findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
      Long userId, RecommendationStatus status);
}
