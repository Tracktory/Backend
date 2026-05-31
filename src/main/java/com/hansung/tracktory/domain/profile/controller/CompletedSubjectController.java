package com.hansung.tracktory.domain.profile.controller;

import com.hansung.tracktory.domain.profile.dto.CompletedSubjectAddRequest;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectDeleteResponse;
import com.hansung.tracktory.domain.profile.dto.CompletedSubjectResponse;
import com.hansung.tracktory.domain.profile.service.CompletedSubjectService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subjects/completed-courses")
@RequiredArgsConstructor
public class CompletedSubjectController {

  private final CompletedSubjectService completedSubjectService;

  @PostMapping
  public ResponseEntity<ApiResponse<CompletedSubjectResponse>> add(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody CompletedSubjectAddRequest request) {
    CompletedSubjectResponse response = completedSubjectService.add(principal.getUserId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<CompletedSubjectDeleteResponse>> delete(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
    CompletedSubjectDeleteResponse response =
        completedSubjectService.delete(principal.getUserId(), id);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
