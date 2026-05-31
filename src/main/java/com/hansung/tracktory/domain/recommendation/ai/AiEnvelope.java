package com.hansung.tracktory.domain.recommendation.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * AI 중계 서버의 공통 응답 envelope — 실제 페이로드는 {@code data} 에 담긴다.
 *
 * <p>FastAPI 계약: {@code {success, data, error}}. {@code error} 는 실패 시에만 채워지며 본 백엔드는 소비하지 않으므로 모델링하지
 * 않고 무시한다(성공 판정·페이로드 추출만 사용).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiEnvelope<T>(boolean success, T data) {}
