package com.hansung.tracktory.domain.recommendation.onboarding;

import java.util.List;

/**
 * 온보딩 도메인이 추천 생성에 노출하는 읽기 전용 스냅샷.
 *
 * <p>온보딩 영속 모델 자체는 별도 작업 범위이며, 추천 도메인은 포트({@link OnboardingProfileReader}) 너머의 구현에만 의존한다. 분류 항목은
 * 카탈로그 {@code code}(관심사·흥미·가치·회사유형은 한글 라벨, 트랙·과목은 식별 코드)로 표현되어 AI 중계 서버 요청과 그대로 정합한다.
 */
public record OnboardingProfileSnapshot(
    Long userId,
    int admissionYear,
    String collegeName,
    String departmentName,
    Integer currentSemester,
    List<String> trackCodes,
    List<String> interestCodes,
    List<String> devFieldCodes,
    List<String> workValueCodes,
    List<String> companyTypeCodes,
    List<String> ncsStudied,
    List<CompletedCourse> completedCourses) {

  /** 이수 과목 한 건 — 과목 식별 코드 + 이수 시점(학년·학기). 과거 학기 로드맵 재구성의 입력. */
  public record CompletedCourse(String subjectCode, int year, int semester) {}
}
