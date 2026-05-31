package com.hansung.tracktory.domain.profile.dto;

import java.util.List;

/** GET /api/v1/me/profile 응답 — 프로필 + 온보딩 정보 전체. */
public record ProfileResponse(
    ProfileSummary profile,
    List<TrackItem> tracks,
    List<CodeItem> interests,
    List<CodeItem> devFields,
    List<CodeItem> companyTypes,
    List<CodeItem> workValues,
    List<TechStackItem> techStacks,
    List<String> techStackCustoms,
    List<CompletedSubjectItem> completedSubjects) {

  public record ProfileSummary(
      Integer currentYear, String name, Long departmentId, String profileImageUrl) {}

  public record TrackItem(Long trackId, String name, Integer trackOrder) {}

  /** id + code 형태의 분류 항목 (관심사·개발분야·회사유형·가치관 공용). */
  public record CodeItem(Long id, String code) {}

  public record TechStackItem(Long id, String name) {}

  public record CompletedSubjectItem(Long subjectId, String name, Integer year, Integer semester) {}
}
