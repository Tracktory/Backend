package com.hansung.tracktory.domain.profile.dto;

public record CompletedSubjectDeleteResponse(Long deletedId) {

  public static CompletedSubjectDeleteResponse of(Long deletedId) {
    return new CompletedSubjectDeleteResponse(deletedId);
  }
}
