package com.hansung.tracktory.domain.profile.dto;

import java.util.List;

public record ProfileUpdateResponse(List<String> updatedFields) {

  public static ProfileUpdateResponse of(List<String> updatedFields) {
    return new ProfileUpdateResponse(updatedFields);
  }
}
