package com.hansung.tracktory.domain.recommendation.entity;

/** 추천 트랜잭션의 노출 상태 — active(최신 노출) / superseded(대체됨) / archived(보관). */
public enum RecommendationStatus {
  ACTIVE,
  SUPERSEDED,
  ARCHIVED
}
