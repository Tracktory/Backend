package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectSemester;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.RemainingCourseView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCourseContribution;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationJobCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationNextAction;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationTriggerSource;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisReportAssemblerTest {

  @InjectMocks private AnalysisReportAssembler assembler;

  @Mock private SubjectRepository subjectRepository;
  @Mock private TrackSubjectRepository trackSubjectRepository;

  @Test
  void assemble_derivesThreeTierPercentsAndFieldCoverageFromSnapshot() {
    Recommendation recommendation = recommendationWithCoverage();
    OnboardingProfileSnapshot profile = profileWithCompleted();
    given(subjectRepository.findByCodeIn(anyCollection())).willReturn(List.of());

    AnalysisReportResponse report = assembler.assemble(recommendation, profile);

    // 카운트는 그대로 보존, 백분율은 covered/required 반올림 — current ≤ nextActions ≤ expected 불변.
    assertThat(report.coverage().currentPercent()).isEqualTo(40);
    assertThat(report.coverage().nextActionsPercent()).isEqualTo(60);
    assertThat(report.coverage().expectedPercent()).isEqualTo(80);
    assertThat(report.coverage().gapTokens()).containsExactly("Kafka", "Redis");
    assertThat(report.coverage().fields())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.jobCode()).isEqualTo("be_dev");
              assertThat(f.currentPercent()).isEqualTo(40);
              assertThat(f.expectedPercent()).isEqualTo(80);
              assertThat(f.missingTokens()).containsExactly("Kafka", "Redis");
            });
    assertThat(report.nextActions())
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.code()).isEqualTo("db");
              assertThat(a.contributionPercent()).isEqualTo(20);
            });
  }

  @Test
  void assemble_remainingCoursesExcludeCompletedAndDistinguishRequiredFromElective() {
    Track track = Track.builder().code("BIGDATA").name("빅데이터트랙").build();
    Recommendation recommendation = recommendationWithPrimaryTrack(track);
    OnboardingProfileSnapshot profile = profileWithCompleted(); // 이수: 자료구조(ds)

    Subject ds = subject("ds", "자료구조", "3.0");
    Subject db = subject("db", "데이터베이스", "3.0");
    Subject ai = subject("ai", "인공지능", "3.0");
    given(trackSubjectRepository.findByTrackIn(anyCollection()))
        .willReturn(
            List.of(
                trackSubject(track, ds, SubjectType.REQUIRED, SubjectStage.FOUNDATION),
                trackSubject(track, db, SubjectType.REQUIRED, SubjectStage.CORE),
                trackSubject(track, ai, SubjectType.ELECTIVE, SubjectStage.APPLIED)));
    given(subjectRepository.findByCodeIn(anyCollection())).willReturn(List.of());

    AnalysisReportResponse report = assembler.assemble(recommendation, profile);

    // 이수한 ds 는 제외, 미이수 db(필수)·ai(선택)만 남고 type 으로 구분된다.
    assertThat(report.remainingCourses())
        .extracting(RemainingCourseView::code)
        .containsExactly("db", "ai");
    assertThat(report.remainingCourses())
        .filteredOn(c -> c.type().equals("REQUIRED"))
        .extracting(RemainingCourseView::code)
        .containsExactly("db");
  }

  @Test
  void assemble_aggregatesCompletedCountAndEarnedCredits() {
    Recommendation recommendation = recommendationWithPrimaryTrack(null);
    OnboardingProfileSnapshot profile = profileWithCompleted(); // 이수: ds 1건
    given(subjectRepository.findByCodeIn(anyCollection()))
        .willReturn(List.of(subject("ds", "자료구조", "3.5")));

    AnalysisReportResponse report = assembler.assemble(recommendation, profile);

    assertThat(report.aggregate().completedCourseCount()).isEqualTo(1);
    assertThat(report.aggregate().earnedCredits()).isEqualTo(3.5);
  }

  @Test
  void assemble_defaultAnchorIsHighestScoreJobAndLabelsCoverage() {
    Recommendation recommendation = recommendationWithJobsAndCoverage();
    OnboardingProfileSnapshot profile = profileWithCompleted();
    given(subjectRepository.findByCodeIn(anyCollection())).willReturn(List.of());

    AnalysisReportResponse report = assembler.assemble(recommendation, profile);

    // 기준 직무 미지정 → 매칭 1순위(score 90) be_dev 가 기준, 헤드라인은 AI 보존 top-level 3단(40/60/80).
    assertThat(report.anchorJob().code()).isEqualTo("be_dev");
    assertThat(report.anchorJob().name()).isEqualTo("백엔드 개발자");
    assertThat(report.coverage().currentPercent()).isEqualTo(40);
    assertThat(report.coverage().nextActionsPercent()).isEqualTo(60);
    assertThat(report.coverage().expectedPercent()).isEqualTo(80);
    assertThat(report.nextActions()).hasSize(1);
  }

  @Test
  void assemble_switchedAnchorReAnchorsCoverageToSelectedJob() {
    Recommendation recommendation = recommendationWithJobsAndCoverage();
    OnboardingProfileSnapshot profile = profileWithCompleted();
    given(subjectRepository.findByCodeIn(anyCollection())).willReturn(List.of());

    AnalysisReportResponse report = assembler.assemble(recommendation, profile, "fe_dev");

    // 기준 직무를 fe_dev 로 바꾸면 그 직무 per-job 충족도(3/5→4/5)로 재anchor, 중간 단은 현재값과 같게 둬 불변을 지킨다.
    assertThat(report.anchorJob().code()).isEqualTo("fe_dev");
    assertThat(report.coverage().currentPercent()).isEqualTo(60);
    assertThat(report.coverage().nextActionsPercent()).isEqualTo(60);
    assertThat(report.coverage().expectedPercent()).isEqualTo(80);
    assertThat(report.coverage().gapTokens()).containsExactly("Vue");
    // 다음 액션·기여 배지는 매칭 1순위 기준 산출물이라 비기본 anchor 에서는 비운다.
    assertThat(report.nextActions()).isEmpty();
    // 분야별 분석은 anchor 와 무관하게 추천 직무 전체를 노출한다.
    assertThat(report.coverage().fields()).hasSize(2);
  }

  @Test
  void assemble_unknownAnchorJobCode_throwsInvalidAnchorJob() {
    Recommendation recommendation = recommendationWithJobsAndCoverage();
    OnboardingProfileSnapshot profile = profileWithCompleted();

    assertThatThrownBy(() -> assembler.assemble(recommendation, profile, "nope"))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ANCHOR_JOB));
  }

  private Recommendation recommendationWithJobsAndCoverage() {
    Recommendation recommendation = baseRecommendation();
    recommendation.addRecommendedJob(
        RecommendedJob.builder().score(90).job(job("be_dev", "백엔드 개발자")).build());
    recommendation.addRecommendedJob(
        RecommendedJob.builder().score(70).job(job("fe_dev", "프론트엔드 개발자")).build());
    RecommendationCoverage coverage =
        RecommendationCoverage.builder()
            .requiredCount(10)
            .currentCovered(4)
            .nextActionsCovered(6)
            .expectedCovered(8)
            .gapTokens("Kafka\nRedis")
            .build();
    recommendation.attachCoverage(coverage);
    coverage.addJobCoverage(
        RecommendationJobCoverage.builder()
            .jobCode("be_dev")
            .jobName("백엔드 개발자")
            .requiredCount(10)
            .currentCovered(4)
            .expectedCovered(8)
            .missingTokens("Kafka\nRedis")
            .build());
    coverage.addJobCoverage(
        RecommendationJobCoverage.builder()
            .jobCode("fe_dev")
            .jobName("프론트엔드 개발자")
            .requiredCount(5)
            .currentCovered(3)
            .expectedCovered(4)
            .missingTokens("Vue")
            .build());
    coverage.addNextAction(
        RecommendationNextAction.builder()
            .orderIndex(0)
            .courseCode("db")
            .courseName("데이터베이스")
            .contributionPercent(20)
            .message("데이터베이스를 들으면 충족도가 오릅니다")
            .build());
    return recommendation;
  }

  private Job job(String code, String name) {
    return Job.builder().code(code).name(name).description("설명").build();
  }

  private Recommendation recommendationWithCoverage() {
    Recommendation recommendation = baseRecommendation();
    RecommendationCoverage coverage =
        RecommendationCoverage.builder()
            .requiredCount(10)
            .currentCovered(4)
            .nextActionsCovered(6)
            .expectedCovered(8)
            .gapTokens("Kafka\nRedis")
            .build();
    recommendation.attachCoverage(coverage);
    coverage.addJobCoverage(
        RecommendationJobCoverage.builder()
            .jobCode("be_dev")
            .jobName("백엔드 개발자")
            .requiredCount(10)
            .currentCovered(4)
            .expectedCovered(8)
            .missingTokens("Kafka\nRedis")
            .build());
    coverage.addNextAction(
        RecommendationNextAction.builder()
            .orderIndex(0)
            .courseCode("db")
            .courseName("데이터베이스")
            .contributionPercent(20)
            .message("데이터베이스를 들으면 충족도가 오릅니다")
            .build());
    coverage.addCourseContribution(
        RecommendationCourseContribution.builder()
            .courseCode("db")
            .courseName("데이터베이스")
            .contributionPercent(20)
            .build());
    return recommendation;
  }

  private Recommendation recommendationWithPrimaryTrack(Track track) {
    Recommendation recommendation = baseRecommendation();
    if (track != null) {
      recommendation.addRecommendedTrack(
          RecommendedTrack.builder()
              .score(90)
              .primary(true)
              .crossCombination(false)
              .track(track)
              .build());
    }
    return recommendation;
  }

  private Recommendation baseRecommendation() {
    return Recommendation.builder()
        .status(RecommendationStatus.ACTIVE)
        .triggerSource(RecommendationTriggerSource.MANUAL)
        .build();
  }

  private OnboardingProfileSnapshot profileWithCompleted() {
    return new OnboardingProfileSnapshot(
        1L,
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
        List.of(new CompletedCourse("ds", 2, 1)));
  }

  private Subject subject(String code, String name, String credit) {
    return Subject.builder()
        .code(code)
        .name(name)
        .description("설명")
        .credit(new BigDecimal(credit))
        .semester(SubjectSemester.FIRST)
        .build();
  }

  private TrackSubject trackSubject(
      Track track, Subject subject, SubjectType type, SubjectStage stage) {
    return TrackSubject.builder().track(track).subject(subject).type(type).stage(stage).build();
  }
}
