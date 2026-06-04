package com.hansung.tracktory.domain.recommendation.service;

import com.hansung.tracktory.domain.catalog.career.repository.JobRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendClient;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendRequest;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.CourseCoverageContribution;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.Explanation;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.JobCoverage;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.JobRationale;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.NextActionSuggestion;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RankedCombo;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapCourse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapStage;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.SemesterPlan;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.TrackRationale;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCourseContribution;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationJobCoverage;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationNextAction;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationTriggerSource;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.entity.Roadmap;
import com.hansung.tracktory.domain.recommendation.entity.RoadmapItem;
import com.hansung.tracktory.domain.recommendation.entity.RoadmapSemester;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileReader;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 추천 생성 오케스트레이션 — 온보딩 프로필을 읽어 AI 중계 서버에 추천을 요청하고, 응답의 직무·트랙·로드맵을 카탈로그 기준으로 매핑해 한 트랜잭션으로 저장한다.
 *
 * <p>새 추천은 직전 활성 추천을 supersede 한다. {@code forceRefresh} 가 아니고 이미 활성 추천이 있으면 AI 재호출 없이 기존 추천을 그대로
 * 조립해 반환한다. 응답 캐싱·비동기 처리는 MVP 범위 밖이다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

  private static final int MAX_SECONDARY_TRACKS = 5;

  /** AI 가 학과 경계를 넘는 이색 조합 슬롯에 부여하는 slot_type 값. */
  private static final String SLOT_CROSS_COLLEGE = "cross_college";

  private final OnboardingProfileReader onboardingProfileReader;
  private final AiRecommendClient aiRecommendClient;
  private final RecommendationAssembler recommendationAssembler;
  private final UserRepository userRepository;
  private final RecommendationRepository recommendationRepository;
  private final JobRepository jobRepository;
  private final TrackRepository trackRepository;
  private final SubjectRepository subjectRepository;

  @Transactional
  public RecommendationResponse generate(Long userId, boolean forceRefresh) {
    OnboardingProfileSnapshot profile =
        onboardingProfileReader
            .read(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

    if (!forceRefresh) {
      Optional<Recommendation> active =
          recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
              userId, RecommendationStatus.ACTIVE);
      if (active.isPresent()) {
        return recommendationAssembler.assemble(active.get(), profile);
      }
    }

    AiRecommendResponse aiResponse = aiRecommendClient.generate(toAiRequest(profile));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

    recommendationRepository
        .findByUser_IdAndStatus(userId, RecommendationStatus.ACTIVE)
        .forEach(Recommendation::markSuperseded);

    Recommendation saved = recommendationRepository.save(buildRecommendation(user, aiResponse));
    return recommendationAssembler.assemble(saved, profile);
  }

  private AiRecommendRequest toAiRequest(OnboardingProfileSnapshot profile) {
    return new AiRecommendRequest(
        profile.admissionYear(),
        profile.collegeName(),
        profile.departmentName(),
        profile.trackCodes(),
        profile.currentSemester(),
        profile.interestCodes(),
        profile.devFieldCodes(),
        profile.workValueCodes(),
        profile.companyTypeCodes(),
        profile.ncsStudied(),
        profile.completedCourses().stream().map(CompletedCourse::subjectCode).toList());
  }

  private Recommendation buildRecommendation(User user, AiRecommendResponse ai) {
    RankedCombo topCombo = first(ai.primaryCombos());
    Recommendation recommendation =
        Recommendation.builder()
            .user(user)
            .status(RecommendationStatus.ACTIVE)
            .triggerSource(RecommendationTriggerSource.MANUAL)
            .trackCombinationScore(topCombo == null ? null : percent(topCombo.synergyScore()))
            .trackCombinationSummary(comboSummary(topCombo))
            .trackCombinationReasoning(comboReasoning(ai.explanation(), topCombo))
            .build();

    addJobs(recommendation, ai);
    addTracks(recommendation, ai, topCombo);
    addRoadmap(recommendation, ai);
    addCoverage(recommendation, ai);
    return recommendation;
  }

  // AI 가 추천 생성 시점에 산출한 역량 충족도 스냅샷을 그대로 보존한다. 비율은 저장하지 않고 카운트만 보존하며,
  // AI 가 합집합으로 계산한 next_actions_covered 도 재계산 없이 그대로 옮겨 화면 3단 표기(현재 ≤ 다음 N개 ≤ 전체) 불변을 지킨다.
  private void addCoverage(Recommendation recommendation, AiRecommendResponse ai) {
    AiRecommendResponse.CoverageAnalysis analysis = ai.coverageAnalysis();
    if (analysis == null) {
      return;
    }
    RecommendationCoverage coverage =
        RecommendationCoverage.builder()
            .requiredCount(analysis.requiredCount())
            .currentCovered(analysis.currentCovered())
            .nextActionsCovered(analysis.nextActionsCovered())
            .expectedCovered(analysis.expectedCovered())
            .gapTokens(joinTokens(analysis.gapTokens()))
            .build();
    recommendation.attachCoverage(coverage);

    for (JobCoverage job : nullSafe(analysis.jobs())) {
      if (job == null || job.jobId() == null) {
        continue;
      }
      coverage.addJobCoverage(
          RecommendationJobCoverage.builder()
              .jobCode(job.jobId())
              .jobName(job.jobName())
              .requiredCount(job.requiredCount())
              .currentCovered(job.currentCovered())
              .expectedCovered(job.expectedCovered())
              .missingTokens(joinTokens(job.missingTokens()))
              .build());
    }

    for (CourseCoverageContribution contribution : nullSafe(analysis.courseContributions())) {
      if (contribution == null || contribution.courseId() == null) {
        continue;
      }
      coverage.addCourseContribution(
          RecommendationCourseContribution.builder()
              .courseCode(contribution.courseId())
              .courseName(contribution.courseName())
              .contributionPercent(percent(contribution.contributionRatio()))
              .build());
    }

    int order = 0;
    for (NextActionSuggestion action : nullSafe(analysis.nextActions())) {
      if (action == null || action.courseId() == null) {
        continue;
      }
      coverage.addNextAction(
          RecommendationNextAction.builder()
              .orderIndex(order++)
              .courseCode(action.courseId())
              .courseName(action.courseName())
              .contributionPercent(percent(action.contributionRatio()))
              .message(action.message())
              .build());
    }
  }

  private static String joinTokens(List<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return null;
    }
    return String.join("\n", tokens);
  }

  private void addJobs(Recommendation recommendation, AiRecommendResponse ai) {
    // AI 서버가 세분화 직무를 카탈로그 코드로 fold 하면 서로 다른 직무가 같은 코드로 겹칠 수 있다.
    // (recommendation_id, job_id) 유니크 제약을 지키도록 코드 기준으로 중복을 제거한다(match_score 내림차순 가정 → 첫 건 채택).
    // AI 가 직무 한 건 단위 개별 근거(job_rationales)를 주면 그것을, 없으면 영역(jobs) 단락으로 폴백한다.
    Map<String, String> rationaleByJobId = jobRationaleIndex(ai.explanation());
    String areaReasoning = explanationBody(ai.explanation(), "jobs");
    Set<String> seen = new HashSet<>();
    for (AiRecommendResponse.JobCandidate job : nullSafe(ai.jobs())) {
      if (job == null || job.jobId() == null || !seen.add(job.jobId())) {
        continue;
      }
      String reasoning = firstNonBlank(rationaleByJobId.get(job.jobId()), areaReasoning);
      jobRepository
          .findByCode(job.jobId())
          .ifPresent(
              catalogJob ->
                  recommendation.addRecommendedJob(
                      RecommendedJob.builder()
                          .score(percent(job.matchScore()))
                          .reasoning(reasoning)
                          .competencyTags(nullSafe(job.competencyTags()))
                          .job(catalogJob)
                          .build()));
    }
  }

  private void addTracks(Recommendation recommendation, AiRecommendResponse ai, RankedCombo top) {
    // AI 가 트랙 조합 한 건 단위 개별 근거(track_rationales)를 주면 조합 안에서 트랙별(A/B)로 바인딩하고,
    // 없으면 영역(tracks) 단락으로 폴백한다.
    Map<String, TrackRationale> rationaleByComboKey = trackRationaleIndex(ai.explanation());
    String areaReasoning = explanationBody(ai.explanation(), "tracks");
    Set<String> seen = new HashSet<>();
    if (top != null && top.combo() != null) {
      List<AiRecommendResponse.Track> pair = pair(top);
      for (int i = 0; i < pair.size(); i++) {
        AiRecommendResponse.Track aiTrack = pair.get(i);
        if (aiTrack == null || !seen.add(aiTrack.trackId())) {
          continue;
        }
        String reasoning = trackReasoning(rationaleByComboKey, top, i, areaReasoning);
        findCatalogTrack(aiTrack.trackId())
            .ifPresent(
                t ->
                    addTrack(
                        recommendation, t, percent(top.synergyScore()), true, false, reasoning));
      }
    }

    // AI 슬롯 예약 규칙상 한 트랙은 한 슬롯에만 속하고 이색 조합(cross_college) 슬롯이 일반 다양성(mmr) 슬롯보다
    // 먼저 오므로, seen 중복 제거로 트랙이 한 번만 저장돼도 이색 조합 분류가 보존된다.
    int secondaryCount = 0;
    for (RankedCombo combo : nullSafe(ai.secondaryCombos())) {
      if (combo == null || combo.combo() == null) {
        continue;
      }
      boolean crossCombination = isCrossCollege(combo);
      List<AiRecommendResponse.Track> pair = pair(combo);
      for (int i = 0; i < pair.size(); i++) {
        AiRecommendResponse.Track aiTrack = pair.get(i);
        if (secondaryCount >= MAX_SECONDARY_TRACKS) {
          break;
        }
        if (aiTrack == null || !seen.add(aiTrack.trackId())) {
          continue;
        }
        Optional<Track> track = findCatalogTrack(aiTrack.trackId());
        if (track.isEmpty()) {
          continue;
        }
        String reasoning = trackReasoning(rationaleByComboKey, combo, i, areaReasoning);
        addTrack(
            recommendation,
            track.get(),
            percent(combo.synergyScore()),
            false,
            crossCombination,
            reasoning);
        secondaryCount++;
      }
    }
  }

  // 조합 한 건의 개별 근거에서 트랙 위치(0=트랙A, 1=트랙B)에 해당하는 근거를 고른다. 항목별 근거가 없거나 비면 영역 단락으로 폴백한다.
  private static String trackReasoning(
      Map<String, TrackRationale> rationaleByComboKey,
      RankedCombo combo,
      int trackIndex,
      String areaReasoning) {
    TrackRationale rationale = rationaleByComboKey.get(comboKey(combo));
    if (rationale == null) {
      return areaReasoning;
    }
    String perTrack = trackIndex == 0 ? rationale.trackARationale() : rationale.trackBRationale();
    return firstNonBlank(perTrack, areaReasoning);
  }

  // 최상위 조합 단위 근거(시너지)를 고른다 — 개별 트랙 근거와 구분되는 조합 전체 근거. 없으면 영역(tracks) 단락으로 폴백한다.
  private static String comboReasoning(Explanation explanation, RankedCombo top) {
    String areaReasoning = explanationBody(explanation, "tracks");
    if (top == null || top.combo() == null) {
      return areaReasoning;
    }
    TrackRationale rationale = trackRationaleIndex(explanation).get(comboKey(top));
    return rationale == null
        ? areaReasoning
        : firstNonBlank(rationale.comboRationale(), areaReasoning);
  }

  private static String comboKey(RankedCombo combo) {
    return combo == null || combo.combo() == null ? null : combo.combo().comboKey();
  }

  private static Map<String, String> jobRationaleIndex(Explanation explanation) {
    Map<String, String> index = new HashMap<>();
    if (explanation == null) {
      return index;
    }
    for (JobRationale rationale : nullSafe(explanation.jobRationales())) {
      if (rationale != null && rationale.jobId() != null) {
        index.putIfAbsent(rationale.jobId(), rationale.rationale());
      }
    }
    return index;
  }

  private static Map<String, TrackRationale> trackRationaleIndex(Explanation explanation) {
    Map<String, TrackRationale> index = new HashMap<>();
    if (explanation == null) {
      return index;
    }
    for (TrackRationale rationale : nullSafe(explanation.trackRationales())) {
      if (rationale != null && rationale.comboKey() != null) {
        index.putIfAbsent(rationale.comboKey(), rationale);
      }
    }
    return index;
  }

  private static String firstNonBlank(String preferred, String fallback) {
    return preferred != null && !preferred.isBlank() ? preferred : fallback;
  }

  // AI 카탈로그와 본 백엔드 카탈로그가 가운뎃점을 서로 다른 유니코드로 적재해(U+00B7 vs U+318D) 가운뎃점을 포함한
  // 트랙 code 의 동등 비교가 실패한다. 원본 code 로 먼저 조회하고, 못 찾으면 가운뎃점을 카탈로그 정규형으로 맞춘 code 로 한 번 더 조회한다.
  // 원본이 이미 매칭되는 트랙은 동작이 바뀌지 않고, 가운뎃점 불일치로 누락되던 트랙만 구제된다.
  private Optional<Track> findCatalogTrack(String aiTrackId) {
    if (aiTrackId == null) {
      return Optional.empty();
    }
    Optional<Track> exact = trackRepository.findByCode(aiTrackId);
    if (exact.isPresent()) {
      return exact;
    }
    String normalized = TrackCodeNormalizer.toCatalogForm(aiTrackId);
    if (normalized.equals(aiTrackId)) {
      return exact;
    }
    return trackRepository.findByCode(normalized);
  }

  private static boolean isCrossCollege(RankedCombo combo) {
    return SLOT_CROSS_COLLEGE.equalsIgnoreCase(combo.slotType());
  }

  private void addTrack(
      Recommendation recommendation,
      Track track,
      int score,
      boolean primary,
      boolean crossCombination,
      String reasoning) {
    recommendation.addRecommendedTrack(
        RecommendedTrack.builder()
            .score(score)
            .primary(primary)
            .crossCombination(crossCombination)
            .reasoning(reasoning)
            .track(track)
            .build());
  }

  private void addRoadmap(Recommendation recommendation, AiRecommendResponse ai) {
    AiRecommendResponse.Roadmap aiRoadmap = ai.roadmap();
    if (aiRoadmap == null) {
      return;
    }
    Roadmap roadmap =
        Roadmap.builder().reasoning(explanationBody(ai.explanation(), "roadmap")).build();
    recommendation.attachRoadmap(roadmap);

    Map<String, SubjectStage> courseStages = courseStageMap(aiRoadmap);
    Map<String, RoadmapSemester> semesterByKey = new LinkedHashMap<>();
    Set<String> addedItems = new HashSet<>();

    for (SemesterPlan plan : nullSafe(aiRoadmap.semesters())) {
      int year = resolveYear(plan);
      int semester = resolveSemester(plan);
      String key = year + "-" + semester;
      RoadmapSemester roadmapSemester =
          semesterByKey.computeIfAbsent(
              key,
              k -> {
                RoadmapSemester created =
                    RoadmapSemester.builder()
                        .year(year)
                        .semester(semester)
                        .stage(semesterStage(plan, courseStages, year))
                        .build();
                roadmap.addSemester(created);
                return created;
              });
      for (RoadmapCourse course : nullSafe(plan.courses())) {
        if (!addedItems.add(key + "|" + course.courseId())) {
          continue;
        }
        subjectRepository
            .findByCode(course.courseId())
            .ifPresent(
                subject ->
                    roadmapSemester.addItem(
                        RoadmapItem.builder()
                            .subject(subject)
                            .score(percent(course.score()))
                            .build()));
      }
    }
  }

  private Map<String, SubjectStage> courseStageMap(AiRecommendResponse.Roadmap aiRoadmap) {
    Map<String, SubjectStage> map = new HashMap<>();
    for (RoadmapStage stage : nullSafe(aiRoadmap.stages())) {
      SubjectStage mapped = mapStageLabel(stage.stage());
      if (mapped == null) {
        continue;
      }
      for (RoadmapCourse course : nullSafe(stage.courses())) {
        map.put(course.courseId(), mapped);
      }
    }
    return map;
  }

  private SubjectStage semesterStage(
      SemesterPlan plan, Map<String, SubjectStage> courseStages, int year) {
    for (RoadmapCourse course : nullSafe(plan.courses())) {
      SubjectStage stage = courseStages.get(course.courseId());
      if (stage != null) {
        return stage;
      }
    }
    return stageForYear(year);
  }

  private static SubjectStage mapStageLabel(String label) {
    if (label == null) {
      return null;
    }
    return switch (label.trim().toLowerCase()) {
      case "foundation" -> SubjectStage.FOUNDATION;
      case "core" -> SubjectStage.CORE;
      case "application", "applied" -> SubjectStage.APPLIED;
      case "industry" -> SubjectStage.INDUSTRY;
      default -> null;
    };
  }

  private static SubjectStage stageForYear(int year) {
    SubjectStage[] byYear = {
      SubjectStage.FOUNDATION, SubjectStage.CORE, SubjectStage.APPLIED, SubjectStage.INDUSTRY
    };
    int clamped = Math.min(Math.max(year, 1), byYear.length);
    return byYear[clamped - 1];
  }

  private static int resolveYear(SemesterPlan plan) {
    int grade = plan.grade();
    if (grade >= 1 && grade <= 4) {
      return grade;
    }
    return clamp((plan.semester() - 1) / 2 + 1, 1, 4);
  }

  private static int resolveSemester(SemesterPlan plan) {
    int semester = plan.semester();
    if (semester == 1 || semester == 2) {
      return semester;
    }
    return clamp((semester - 1) % 2 + 1, 1, 2);
  }

  private List<AiRecommendResponse.Track> pair(RankedCombo combo) {
    AiRecommendResponse.TrackCombo trackCombo = combo.combo();
    if (trackCombo == null) {
      return List.of();
    }
    return java.util.Arrays.asList(trackCombo.trackA(), trackCombo.trackB());
  }

  private static String comboSummary(RankedCombo top) {
    if (top == null || top.combo() == null) {
      return null;
    }
    String a = top.combo().trackA() == null ? null : top.combo().trackA().trackName();
    String b = top.combo().trackB() == null ? null : top.combo().trackB().trackName();
    if (a == null && b == null) {
      return null;
    }
    return (a == null ? "" : a) + " + " + (b == null ? "" : b);
  }

  private static String explanationBody(Explanation explanation, String topic) {
    if (explanation == null) {
      return null;
    }
    if (explanation.sections() != null) {
      for (AiRecommendResponse.ExplanationSection section : explanation.sections()) {
        if (section != null && topic.equalsIgnoreCase(section.topic())) {
          return section.body();
        }
      }
    }
    return explanation.text();
  }

  private static int percent(double score) {
    return clamp((int) Math.round(score * 100), 0, 100);
  }

  private static int clamp(int value, int min, int max) {
    return Math.min(Math.max(value, min), max);
  }

  private static <T> List<T> nullSafe(List<T> list) {
    return list == null ? List.of() : list;
  }

  private static <T> T first(List<T> list) {
    return list == null || list.isEmpty() ? null : list.get(0);
  }
}
