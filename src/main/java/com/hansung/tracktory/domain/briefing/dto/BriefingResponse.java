package com.hansung.tracktory.domain.briefing.dto;

import java.util.List;

/**
 * 직무 브리핑 조회 API 의 프론트엔드 응답 — 활성 추천 직무에 연결된 트렌드 카드 묶음을 전달한다.
 *
 * <p>홈 브리핑 시트가 소비하며, 카드는 추천 직무 순서(적합도 내림차순)를 따른다. 큐레이션이 없는 직무는 빠지므로 카드 수가 추천 직무 수보다 적을 수 있고, 매칭이
 * 하나도 없으면 빈 리스트다.
 */
public record BriefingResponse(List<BriefingCardView> briefings) {

  /**
   * 직무 한 건의 트렌드 브리핑 카드. {@code code} 는 직무 카탈로그 표준 코드(추천 직무 식별자와 정합), {@code skills} 는 카드가 강조하는 필수
   * 역량·기술 키워드(없으면 빈 리스트), {@code sources} 는 검증 가능한 출처(최소 1개).
   */
  public record BriefingCardView(
      String code,
      String name,
      String headline,
      String summary,
      List<String> skills,
      List<SourceView> sources) {}

  /** 브리핑 근거 출처. {@code publishedAt} 은 발행 시점(YYYY 또는 YYYY-MM), 미상이면 null. */
  public record SourceView(String title, String url, String publishedAt) {}
}
