package com.hansung.tracktory.domain.briefing.ai;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * AI 중계 서버의 직무 브리핑 요청 바디 — FastAPI BriefingRequest 를 mirror 한다.
 *
 * <p>브리핑 표면은 추천 입력 전체가 아니라 직무 식별자만 받는다. camelCase 자바 필드 {@code jobIds} 는 snake_case JSON {@code
 * job_ids} 로 직렬화된다. FastAPI 계약상 최소 1개이며 각 코드는 공백 제거 후 1자 이상이어야 하므로, 빈 목록을 보내지 않는 책임은 호출 서비스에 있다.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiBriefingRequest(List<String> jobIds) {}
