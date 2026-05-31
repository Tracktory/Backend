package com.hansung.tracktory.domain.recommendation.service;

import com.hansung.tracktory.domain.catalog.career.repository.JobRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendClient;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendRequest;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.Explanation;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RankedCombo;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapCourse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapStage;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.SemesterPlan;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
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
            .trackCombinationReasoning(explanationBody(ai.explanation(), "tracks"))
            .build();

    addJobs(recommendation, ai);
    addTracks(recommendation, ai, topCombo);
    addRoadmap(recommendation, ai);
    return recommendation;
  }

  private void addJobs(Recommendation recommendation, AiRecommendResponse ai) {
    // AI 서버가 세분화 직무를 카탈로그 코드로 fold 하면 서로 다른 직무가 같은 코드로 겹칠 수 있다.
    // (recommendation_id, job_id) 유니크 제약을 지키도록 코드 기준으로 중복을 제거한다(match_score 내림차순 가정 → 첫 건 채택).
    Set<String> seen = new HashSet<>();
    for (AiRecommendResponse.JobCandidate job : nullSafe(ai.jobs())) {
      if (job == null || job.jobId() == null || !seen.add(job.jobId())) {
        continue;
      }
      jobRepository
          .findByCode(job.jobId())
          .ifPresent(
              catalogJob ->
                  recommendation.addRecommendedJob(
                      RecommendedJob.builder()
                          .score(percent(job.matchScore()))
                          .job(catalogJob)
                          .build()));
    }
  }

  private void addTracks(Recommendation recommendation, AiRecommendResponse ai, RankedCombo top) {
    Set<String> seen = new HashSet<>();
    if (top != null && top.combo() != null) {
      for (AiRecommendResponse.Track aiTrack : pair(top)) {
        if (aiTrack == null || !seen.add(aiTrack.trackId())) {
          continue;
        }
        Optional<Track> track = trackRepository.findByCode(aiTrack.trackId());
        track.ifPresent(t -> addTrack(recommendation, t, percent(top.synergyScore()), true));
      }
    }

    int secondaryCount = 0;
    for (RankedCombo combo : nullSafe(ai.secondaryCombos())) {
      if (combo == null || combo.combo() == null) {
        continue;
      }
      for (AiRecommendResponse.Track aiTrack : pair(combo)) {
        if (secondaryCount >= MAX_SECONDARY_TRACKS) {
          break;
        }
        if (aiTrack == null || !seen.add(aiTrack.trackId())) {
          continue;
        }
        Optional<Track> track = trackRepository.findByCode(aiTrack.trackId());
        if (track.isEmpty()) {
          continue;
        }
        addTrack(recommendation, track.get(), percent(combo.synergyScore()), false);
        secondaryCount++;
      }
    }
  }

  private void addTrack(Recommendation recommendation, Track track, int score, boolean primary) {
    recommendation.addRecommendedTrack(
        RecommendedTrack.builder().score(score).primary(primary).track(track).build());
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
