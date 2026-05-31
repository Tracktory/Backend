package com.hansung.tracktory.domain.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;

@Getter
public class OnboardingRequest {
  @NotNull @Valid private ProfileRequest profile; // 프로필

  @Size(max = 2) @Valid private List<TrackRequest> tracks; // 트랙 (1학년은 선택, 보내면 1트랙 필수)

  @NotNull @Size(min = 1, max = 5) private List<Long> interestIds; // 관심 분야

  @NotNull @Size(min = 1) private List<Long> companyTypeIds; // 희망 회사 유형

  @NotNull @Size(min = 1, max = 3) private List<Long> workValueIds; // 가치관

  @Size(max = 3) private List<Long> devFieldIds; // 개발 분야 (선택)

  private List<Long> techStackIds; // 기술 스택 (선택)

  private List<String> techStackCustoms; // 기술 스택 자유 입력 (선택)

  @Valid private List<CompletedSubjectRequest> completedSubjects; // 이수 과목 (선택)

  @Getter
  public static class ProfileRequest {
    @NotNull @Min(1) @Max(4) private Integer currentYear; // 학년

    @NotBlank @Size(max = 50) private String name; // 이름

    @NotNull private Long departmentId; // 학부 ID
  }

  @Getter
  public static class TrackRequest {
    @NotNull private Long trackId; // 트랙 ID

    @NotNull @Min(1) @Max(2) private Integer trackOrder; // 1=1트랙, 2=트랙
  }

  @Getter
  public static class CompletedSubjectRequest {
    @NotNull private Long subjectId; // 과목 ID

    @NotNull @Min(1) @Max(4) private Integer year; // 이수 학년

    @NotNull @Min(1) @Max(2) private Integer semester; // 이수 학기
  }
}
