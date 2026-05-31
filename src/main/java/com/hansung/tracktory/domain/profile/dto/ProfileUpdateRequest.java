package com.hansung.tracktory.domain.profile.dto;

import com.hansung.tracktory.domain.profile.dto.OnboardingRequest.TrackRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;

@Getter
public class ProfileUpdateRequest {
  @Valid private ProfilePart profile; // 프로필 (부분 수정)

  @Size(max = 2) @Valid private List<TrackRequest> tracks; // 트랙 (전체 교체)

  @Size(max = 5) private List<Long> interestIds; // 관심 분야 (전체 교체)

  @Size(max = 3) private List<Long> devFieldIds; // 개발 분야 (전체 교체)

  private List<Long> companyTypeIds; // 희망 회사 유형 (전체 교체)

  @Size(max = 3) private List<Long> workValueIds; // 가치관 (전체 교체)

  private List<Long> techStackIds; // 기술 스택 (전체 교체)

  private List<String> techStackCustoms; // 기술 스택 자유 입력 (전체 교체)

  @Getter
  public static class ProfilePart {
    @Min(1) @Max(4) private Integer currentYear; // 학년

    @Size(max = 50) private String name; // 이름
  }
}
