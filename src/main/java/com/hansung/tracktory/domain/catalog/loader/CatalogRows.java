package com.hansung.tracktory.domain.catalog.loader;

import com.hansung.tracktory.domain.catalog.career.entity.JobTechStackStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import java.math.BigDecimal;
import java.util.List;

/** 카탈로그 파싱의 중간 표현(순수 데이터). 영속 엔티티로 변환되기 전 단계로, DB 의존 없이 단위 테스트된다. */
public final class CatalogRows {

  private CatalogRows() {}

  /** 트랙 한 건 — 소속 단과대/학부 이름과 AI 정합용 {@code code}. */
  public record TrackRow(String code, String collegeName, String departmentName, String name) {}

  /** 과목 한 건 — 트랙 연결({@code trackCodes})과 졸업요건 유형/학습 단계 포함. */
  public record CourseRow(
      String code,
      String name,
      BigDecimal credit,
      SubjectSemester semester,
      SubjectType type,
      SubjectStage stage,
      List<String> trackCodes) {}

  /** 직무가 요구하는 기술 한 건과 그 학습 단계. */
  public record TechStackRow(String name, JobTechStackStage stage) {}

  /** 직무 한 건과 요구 기술 목록. */
  public record JobRow(
      String code, String name, String description, List<TechStackRow> techStacks) {}

  /** 온보딩 분류 taxonomy 전체(관심사/개발분야/회사유형/가치관). */
  public record ClassificationData(
      List<String> interests,
      List<String> devFields,
      List<String> companyTypes,
      List<String> workValues) {}
}
