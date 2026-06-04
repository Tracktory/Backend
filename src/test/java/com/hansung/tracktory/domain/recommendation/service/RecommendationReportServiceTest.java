package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileReader;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationReportServiceTest {

  private static final long USER_ID = 1L;

  @InjectMocks private RecommendationReportService reportService;

  @Mock private OnboardingProfileReader onboardingProfileReader;
  @Mock private RecommendationRepository recommendationRepository;
  @Mock private AnalysisReportAssembler analysisReportAssembler;

  @Test
  void getReport_onboardingMissing_throwsOnboardingNotFound() {
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.getReport(USER_ID, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_NOT_FOUND));
    verify(analysisReportAssembler, never()).assemble(any(), any(), any());
  }

  @Test
  void getReport_noActiveRecommendation_throwsRecommendationNotFound() {
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(sampleProfile()));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> reportService.getReport(USER_ID, null))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RECOMMENDATION_NOT_FOUND));
    verify(analysisReportAssembler, never()).assemble(any(), any(), any());
  }

  @Test
  void getReport_activeRecommendation_assemblesFromSameAggregate() {
    OnboardingProfileSnapshot profile = sampleProfile();
    Recommendation active = Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    AnalysisReportResponse expected =
        new AnalysisReportResponse(7L, null, null, null, List.of(), List.of());
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.of(active));
    given(analysisReportAssembler.assemble(active, profile, null)).willReturn(expected);

    AnalysisReportResponse result = reportService.getReport(USER_ID, null);

    assertThat(result).isSameAs(expected);
    verify(analysisReportAssembler).assemble(eq(active), eq(profile), isNull());
  }

  @Test
  void getReport_anchorJobCode_passedThroughToAssembler() {
    OnboardingProfileSnapshot profile = sampleProfile();
    Recommendation active = Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    AnalysisReportResponse expected =
        new AnalysisReportResponse(7L, null, null, null, List.of(), List.of());
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.of(active));
    given(analysisReportAssembler.assemble(active, profile, "be_dev")).willReturn(expected);

    AnalysisReportResponse result = reportService.getReport(USER_ID, "be_dev");

    assertThat(result).isSameAs(expected);
    verify(analysisReportAssembler).assemble(eq(active), eq(profile), eq("be_dev"));
  }

  private OnboardingProfileSnapshot sampleProfile() {
    return new OnboardingProfileSnapshot(
        USER_ID,
        2024,
        "IT공과대학",
        "컴퓨터공학부",
        4,
        List.of(),
        List.of("IT/인터넷"),
        List.of("백엔드"),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }
}
