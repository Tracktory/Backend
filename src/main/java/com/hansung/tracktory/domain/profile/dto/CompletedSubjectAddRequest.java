package com.hansung.tracktory.domain.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CompletedSubjectAddRequest {

  @NotBlank private String subjectName; // 과목 이름

  @NotNull @Min(1) @Max(4) private Integer year; // 이수 학년

  @NotNull @Min(1) @Max(2) private Integer semester; // 이수 학기 (1=1학기, 2=2학기)
}
