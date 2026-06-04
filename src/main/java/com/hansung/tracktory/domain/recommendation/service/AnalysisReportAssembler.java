package com.hansung.tracktory.domain.recommendation.service;

import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.AggregateView;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.AnchorJobView;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.CoverageView;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.FieldCoverageView;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.NextActionView;
import com.hansung.tracktory.domain.recommendation.dto.AnalysisReportResponse.RemainingCourseView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCourseContribution;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationJobCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationNextAction;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 영속된 추천 aggregate(역량 충족도 스냅샷 포함)와 온보딩 스냅샷을 합쳐 상세 분석 리포트 응답으로 조립한다.
 *
 * <p>충족도·분야별 분석·다음 액션은 추천 생성 시점에 AI 가 산출해 보존한 값을 그대로 노출하고(비율만 조회 시점에 카운트로 파생), 잔여 과목·이수 수·취득 학점은
 * 카탈로그와 이수 이력으로 계산한다. 잔여 과목은 주 추천 트랙의 졸업요건 과목 중 미이수분을 과목 단위로 묶어, 한 과목이 여러 트랙에 걸치면 기여 트랙을 함께 노출한다.
 * 모든 lazy 연관 접근은 호출 측 트랜잭션 안에서 이뤄진다.
 */
@Component
@RequiredArgsConstructor
public class AnalysisReportAssembler {

  private final SubjectRepository subjectRepository;
  private final TrackSubjectRepository trackSubjectRepository;

  AnalysisReportResponse assemble(
      Recommendation recommendation, OnboardingProfileSnapshot profile) {
    return assemble(recommendation, profile, null);
  }

  /**
   * 기준 직무({@code anchorJobCode})를 받아 그 직무 기준으로 충족도를 조립한다. null/공백이면 매칭 1순위 직무가 기준이 된다. 추천 직무 목록에 없는
   * 코드는 {@code INVALID_ANCHOR_JOB} 로 거절한다. 기준 직무를 바꾼 경우 다음 액션·기여 배지는 매칭 1순위 기준 산출물이라 비운다.
   */
  AnalysisReportResponse assemble(
      Recommendation recommendation, OnboardingProfileSnapshot profile, String anchorJobCode) {
    RecommendationCoverage coverage = recommendation.getCoverage();
    ResolvedAnchor anchor = resolveAnchor(recommendation, anchorJobCode);
    boolean defaultAnchor = anchor == null || anchor.isDefault();
    return new AnalysisReportResponse(
        recommendation.getId(),
        anchor == null ? null : new AnchorJobView(anchor.code(), anchor.name()),
        coverage(coverage, anchor),
        aggregate(profile),
        remainingCourses(
            recommendation, profile, defaultAnchor ? contributionIndex(coverage) : Map.of()),
        defaultAnchor ? nextActions(coverage) : List.of());
  }

  // 충족도 산출 기준 직무를 정한다. 기본값은 match_score(=score) 1순위 추천 직무. anchorJobCode 가 주어지면 그 직무를 기준으로 하되,
  // 추천 직무 목록에 없으면 거절한다. 추천 직무가 없으면 null (커버리지 자체가 비는 케이스).
  private ResolvedAnchor resolveAnchor(Recommendation recommendation, String anchorJobCode) {
    RecommendedJob defaultJob =
        recommendation.getRecommendedJobs().stream()
            .max(Comparator.comparingInt(RecommendedJob::getScore))
            .orElse(null);
    boolean requested = anchorJobCode != null && !anchorJobCode.isBlank();
    if (defaultJob == null) {
      if (requested) {
        throw new BusinessException(ErrorCode.INVALID_ANCHOR_JOB);
      }
      return null;
    }
    if (!requested) {
      return ResolvedAnchor.of(defaultJob, true);
    }
    RecommendedJob selected =
        recommendation.getRecommendedJobs().stream()
            .filter(job -> anchorJobCode.equals(job.getJob().getCode()))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_ANCHOR_JOB));
    boolean isDefault = selected.getJob().getCode().equals(defaultJob.getJob().getCode());
    return ResolvedAnchor.of(selected, isDefault);
  }

  private CoverageView coverage(RecommendationCoverage coverage, ResolvedAnchor anchor) {
    if (coverage == null) {
      return new CoverageView(0, 0, 0, 0, 0, 0, 0, List.of(), List.of());
    }
    List<FieldCoverageView> fields =
        coverage.getJobCoverages().stream()
            .map(this::fieldCoverage)
            .sorted(Comparator.comparingInt(FieldCoverageView::currentPercent).reversed())
            .toList();
    if (anchor == null || anchor.isDefault()) {
      // 기본 anchor(매칭 1순위) 기준 — AI 가 합집합으로 산출한 다음 N개 충족(중간 단)까지 그대로 노출.
      return new CoverageView(
          coverage.getRequiredCount(),
          coverage.getCurrentCovered(),
          coverage.getNextActionsCovered(),
          coverage.getExpectedCovered(),
          percent(coverage.getCurrentCovered(), coverage.getRequiredCount()),
          percent(coverage.getNextActionsCovered(), coverage.getRequiredCount()),
          percent(coverage.getExpectedCovered(), coverage.getRequiredCount()),
          splitTokens(coverage.getGapTokens()),
          fields);
    }
    // 기준 직무를 바꾼 경우 — 그 직무의 분야별 충족도(현재→전체)로 헤드라인을 재anchor 한다. 다음 N개(중간 단)는
    // 매칭 1순위 기준 합집합이라 그 직무에 유효하지 않으므로 현재값으로 둬 current ≤ nextActions ≤ expected 불변만 지킨다.
    RecommendationJobCoverage job =
        coverage.getJobCoverages().stream()
            .filter(candidate -> anchor.code().equals(candidate.getJobCode()))
            .findFirst()
            .orElse(null);
    if (job == null) {
      return new CoverageView(0, 0, 0, 0, 0, 0, 0, List.of(), fields);
    }
    int currentPercent = percent(job.getCurrentCovered(), job.getRequiredCount());
    return new CoverageView(
        job.getRequiredCount(),
        job.getCurrentCovered(),
        job.getCurrentCovered(),
        job.getExpectedCovered(),
        currentPercent,
        currentPercent,
        percent(job.getExpectedCovered(), job.getRequiredCount()),
        splitTokens(job.getMissingTokens()),
        fields);
  }

  private FieldCoverageView fieldCoverage(RecommendationJobCoverage job) {
    return new FieldCoverageView(
        job.getJobCode(),
        job.getJobName(),
        job.getRequiredCount(),
        job.getCurrentCovered(),
        job.getExpectedCovered(),
        percent(job.getCurrentCovered(), job.getRequiredCount()),
        percent(job.getExpectedCovered(), job.getRequiredCount()),
        splitTokens(job.getMissingTokens()));
  }

  private AggregateView aggregate(OnboardingProfileSnapshot profile) {
    List<String> codes =
        profile.completedCourses().stream().map(CompletedCourse::subjectCode).distinct().toList();
    BigDecimal earned = BigDecimal.ZERO;
    if (!codes.isEmpty()) {
      for (Subject subject : subjectRepository.findByCodeIn(codes)) {
        earned = earned.add(subject.getCredit());
      }
    }
    return new AggregateView(codes.size(), earned.doubleValue());
  }

  private List<RemainingCourseView> remainingCourses(
      Recommendation recommendation,
      OnboardingProfileSnapshot profile,
      Map<String, Integer> contributions) {
    List<Track> primaryTracks =
        recommendation.getRecommendedTracks().stream()
            .filter(RecommendedTrack::isPrimary)
            .map(RecommendedTrack::getTrack)
            .toList();
    if (primaryTracks.isEmpty()) {
      return List.of();
    }
    Set<String> completed =
        profile.completedCourses().stream()
            .map(CompletedCourse::subjectCode)
            .collect(Collectors.toSet());

    Map<String, RemainingAccumulator> grouped = new LinkedHashMap<>();
    for (TrackSubject link : trackSubjectRepository.findByTrackIn(primaryTracks)) {
      Subject subject = link.getSubject();
      if (completed.contains(subject.getCode())) {
        continue;
      }
      grouped.computeIfAbsent(subject.getCode(), k -> new RemainingAccumulator(subject)).add(link);
    }
    return grouped.values().stream()
        .map(acc -> acc.toView(contributions.get(acc.subject.getCode())))
        .toList();
  }

  private List<NextActionView> nextActions(RecommendationCoverage coverage) {
    if (coverage == null) {
      return List.of();
    }
    return coverage.getNextActions().stream()
        .sorted(Comparator.comparingInt(RecommendationNextAction::getOrderIndex))
        .map(
            action ->
                new NextActionView(
                    action.getCourseCode(),
                    action.getCourseName(),
                    action.getContributionPercent(),
                    action.getMessage()))
        .toList();
  }

  private Map<String, Integer> contributionIndex(RecommendationCoverage coverage) {
    if (coverage == null) {
      return Map.of();
    }
    Map<String, Integer> index = new LinkedHashMap<>();
    for (RecommendationCourseContribution contribution : coverage.getCourseContributions()) {
      index.putIfAbsent(contribution.getCourseCode(), contribution.getContributionPercent());
    }
    return index;
  }

  private static int percent(int covered, int required) {
    if (required <= 0) {
      return 0;
    }
    return (int) Math.round(covered * 100.0 / required);
  }

  private static List<String> splitTokens(String joined) {
    if (joined == null || joined.isBlank()) {
      return List.of();
    }
    return List.of(joined.split("\n"));
  }

  /** 충족도 산출 기준 직무 — 코드·이름과 매칭 1순위(기본) 여부. */
  private record ResolvedAnchor(String code, String name, boolean isDefault) {
    private static ResolvedAnchor of(RecommendedJob job, boolean isDefault) {
      return new ResolvedAnchor(job.getJob().getCode(), job.getJob().getName(), isDefault);
    }
  }

  /** 같은 과목이 여러 주 추천 트랙에 걸칠 때 기여 트랙·졸업요건 유형·학습 단계를 한 과목으로 묶는 누적기. */
  private static final class RemainingAccumulator {
    private final Subject subject;
    private final List<String> tracks = new ArrayList<>();
    private SubjectType type;
    private SubjectStage stage;

    private RemainingAccumulator(Subject subject) {
      this.subject = subject;
    }

    private void add(TrackSubject link) {
      tracks.add(link.getTrack().getName());
      if (type == null || typeRank(link.getType()) > typeRank(type)) {
        type = link.getType();
      }
      if (stage == null || link.getStage().ordinal() < stage.ordinal()) {
        stage = link.getStage();
      }
    }

    private RemainingCourseView toView(Integer contributionPercent) {
      return new RemainingCourseView(
          subject.getCode(),
          subject.getName(),
          subject.getCredit().doubleValue(),
          type == null ? null : type.name(),
          stage == null ? null : stage.name(),
          List.copyOf(tracks),
          contributionPercent);
    }

    // 필수 잔여만 추릴 수 있도록 졸업요건 유형 우선순위를 둔다 (전공필수 > 전공선택 > 전공기초).
    private static int typeRank(SubjectType type) {
      return switch (type) {
        case REQUIRED -> 2;
        case ELECTIVE -> 1;
        case BASIC -> 0;
      };
    }
  }
}
