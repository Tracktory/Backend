package com.hansung.tracktory.domain.recommendation.ai;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * AI 중계 서버의 추천 요청 바디 — FastAPI RecommendRequest 11 필드를 mirror 한다. camelCase 자바 필드는 snake_case JSON
 * 으로 직렬화된다 (예: admissionYear → admission_year).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiRecommendRequest(
    int admissionYear,
    String college,
    String department,
    List<String> currentTracks,
    Integer currentSemester,
    List<String> interests,
    List<String> devInterests,
    List<String> workValues,
    List<String> companyTypes,
    List<String> ncsStudied,
    List<String> completedCourses) {}
