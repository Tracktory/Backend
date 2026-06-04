package com.hansung.tracktory.domain.briefing.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * AI 중계 서버 직무 브리핑 응답의 data 본문 — FastAPI BriefingResponse 와 중첩 모델(JobBriefing/BriefingSource)을
 * mirror 한다. 모든 레코드는 snake_case JSON 매핑 + 알 수 없는 필드 무시를 적용한다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiBriefingResponse(List<Briefing> briefings) {

  /**
   * 추천 직무 한 건에 연결된 트렌드 브리핑 카드. {@code jobId} 는 직무 카탈로그 표준 코드(추천 직무 식별자와 정합), {@code sources} 는 검증
   * 가능한 출처(FastAPI 가 최소 1개를 보장).
   */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Briefing(
      String jobId,
      String jobName,
      String headline,
      String summary,
      List<String> skills,
      List<Source> sources) {}

  /** 브리핑 근거 출처. {@code publishedAt} 은 발행 시점(YYYY 또는 YYYY-MM, 미상이면 null). */
  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Source(String title, String url, String publishedAt) {}
}
