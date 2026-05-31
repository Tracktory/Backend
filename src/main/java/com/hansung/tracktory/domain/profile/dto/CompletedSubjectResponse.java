package com.hansung.tracktory.domain.profile.dto;

import com.hansung.tracktory.domain.profile.entity.UserCompletedSubject;

public record CompletedSubjectResponse(Long id, Long subjectId, Integer year, Integer semester) {

  public static CompletedSubjectResponse from(UserCompletedSubject entity) {
    return new CompletedSubjectResponse(
        entity.getId(), entity.getSubject().getId(), entity.getYear(), entity.getSemester());
  }
}
