package com.hansung.tracktory.domain.recommendation.service;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTechStack;
import com.hansung.tracktory.domain.catalog.career.repository.JobTechStackRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectPrerequisite;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectPrerequisiteRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.CourseView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.JobView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.MainSubjectView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.PrerequisiteView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.RoadmapView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.SemesterView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.TrackRecommendationView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.TrackView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.entity.Roadmap;
import com.hansung.tracktory.domain.recommendation.entity.RoadmapSemester;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 영속된 추천 aggregate 와 온보딩 스냅샷을 합쳐 프론트엔드 응답으로 조립한다.
 *
 * <p>AI 중계 서버는 미래(미이수) 학기만 돌려주므로, 과거 학기는 사용자의 이수 이력에서 학년·학기 기준으로 재구성해 앞에 붙인다. 각 과목에는 시점(과거/현재/미래),
 * 이수 여부, 선수 과목을 부착한다. 모든 lazy 연관 접근은 호출 측 트랜잭션 안에서 이뤄진다.
 */
@Component
@RequiredArgsConstructor
public class RecommendationAssembler {

  private static final SubjectStage[] STAGE_BY_YEAR = {
    SubjectStage.FOUNDATION, SubjectStage.CORE, SubjectStage.APPLIED, SubjectStage.INDUSTRY
  };

  /** 트랙별 주요 과목으로 노출할 전공필수 과목 수. */
  private static final int MAIN_SUBJECT_LIMIT = 3;

  private final SubjectRepository subjectRepository;
  private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
  private final TrackSubjectRepository trackSubjectRepository;
  private final JobTechStackRepository jobTechStackRepository;

  RecommendationResponse assemble(
      Recommendation recommendation, OnboardingProfileSnapshot profile) {
    return new RecommendationResponse(
        recommendation.getId(),
        jobs(recommendation),
        tracks(recommendation),
        roadmap(recommendation, profile));
  }

  private List<JobView> jobs(Recommendation recommendation) {
    List<Job> jobs = recommendation.getRecommendedJobs().stream().map(j -> j.getJob()).toList();
    Map<Long, List<String>> techStacks = techStackIndex(jobs);
    return recommendation.getRecommendedJobs().stream()
        .map(
            j ->
                new JobView(
                    j.getJob().getCode(),
                    j.getJob().getName(),
                    // 저장된 내부 점수는 그대로 두고 응답 노출값만 체감 척도로 보정한다.
                    JobScoreCalibrator.toDisplayScore(j.getScore()),
                    j.getReasoning(),
                    techStacks.getOrDefault(j.getJob().getId(), List.of())))
        .toList();
  }

  private Map<Long, List<String>> techStackIndex(List<Job> jobs) {
    Map<Long, List<String>> index = new LinkedHashMap<>();
    if (jobs.isEmpty()) {
      return index;
    }
    for (JobTechStack link : jobTechStackRepository.findByJobIn(jobs)) {
      index
          .computeIfAbsent(link.getJob().getId(), k -> new ArrayList<>())
          .add(link.getTechStack().getName());
    }
    index.values().forEach(names -> names.sort(Comparator.naturalOrder()));
    return index;
  }

  private TrackRecommendationView tracks(Recommendation recommendation) {
    List<Track> tracks =
        recommendation.getRecommendedTracks().stream().map(rt -> rt.getTrack()).toList();
    Map<Long, List<MainSubjectView>> mainSubjects = mainSubjectIndex(tracks);

    List<TrackView> primary = new ArrayList<>();
    List<TrackView> secondary = new ArrayList<>();
    for (RecommendedTrack rt : recommendation.getRecommendedTracks()) {
      TrackView view =
          new TrackView(
              rt.getTrack().getCode(),
              rt.getTrack().getName(),
              rt.getScore(),
              rt.getReasoning(),
              rt.isPrimary(),
              rt.isCrossCombination(),
              mainSubjects.getOrDefault(rt.getTrack().getId(), List.of()));
      (rt.isPrimary() ? primary : secondary).add(view);
    }
    return new TrackRecommendationView(
        recommendation.getTrackCombinationScore(),
        recommendation.getTrackCombinationSummary(),
        recommendation.getTrackCombinationReasoning(),
        primary,
        secondary);
  }

  private Map<Long, List<MainSubjectView>> mainSubjectIndex(List<Track> tracks) {
    Map<Long, List<MainSubjectView>> index = new LinkedHashMap<>();
    if (tracks.isEmpty()) {
      return index;
    }
    Map<Long, List<TrackSubject>> grouped = new LinkedHashMap<>();
    for (TrackSubject link :
        trackSubjectRepository.findByTrackInAndType(tracks, SubjectType.REQUIRED)) {
      grouped.computeIfAbsent(link.getTrack().getId(), k -> new ArrayList<>()).add(link);
    }
    Comparator<TrackSubject> byStageThenCode =
        Comparator.comparingInt((TrackSubject ts) -> ts.getStage().ordinal())
            .thenComparing(ts -> ts.getSubject().getCode());
    grouped.forEach(
        (trackId, links) ->
            index.put(
                trackId,
                links.stream()
                    .sorted(byStageThenCode)
                    .limit(MAIN_SUBJECT_LIMIT)
                    .map(
                        ts ->
                            new MainSubjectView(
                                ts.getSubject().getCode(), ts.getSubject().getName()))
                    .toList()));
    return index;
  }

  private RoadmapView roadmap(Recommendation recommendation, OnboardingProfileSnapshot profile) {
    Roadmap roadmap = recommendation.getRoadmap();
    Integer currentSemester = profile.currentSemester();

    List<PastGroup> pastGroups = groupCompletedCourses(profile);
    List<Subject> allSubjects = new ArrayList<>();
    pastGroups.forEach(g -> allSubjects.addAll(g.subjects()));
    if (roadmap != null) {
      roadmap.getSemesters().stream()
          .flatMap(s -> s.getItems().stream())
          .forEach(item -> allSubjects.add(item.getSubject()));
    }

    Map<Long, List<PrerequisiteView>> prerequisites = prerequisiteIndex(allSubjects);
    Map<Long, SubjectStage> pastStages = pastStageIndex(pastGroups);

    List<SemesterView> semesters = new ArrayList<>();
    for (PastGroup group : pastGroups) {
      semesters.add(pastSemester(group, currentSemester, prerequisites, pastStages));
    }
    if (roadmap != null) {
      for (RoadmapSemester semester : roadmap.getSemesters()) {
        semesters.add(futureSemester(semester, currentSemester, prerequisites));
      }
    }
    return new RoadmapView(roadmap == null ? null : roadmap.getReasoning(), semesters);
  }

  private SemesterView pastSemester(
      PastGroup group,
      Integer currentSemester,
      Map<Long, List<PrerequisiteView>> prerequisites,
      Map<Long, SubjectStage> pastStages) {
    String timing = timing(group.absoluteIndex(), currentSemester, "PAST");
    SubjectStage stage =
        group.subjects().stream()
            .map(s -> pastStages.get(s.getId()))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(stageForYear(group.year()));
    List<CourseView> courses =
        group.subjects().stream()
            .map(
                s ->
                    new CourseView(
                        s.getCode(),
                        s.getName(),
                        timing,
                        true,
                        null,
                        prerequisites.getOrDefault(s.getId(), List.of())))
            .toList();
    return new SemesterView(group.year(), group.semester(), stage.name(), timing, courses);
  }

  private SemesterView futureSemester(
      RoadmapSemester semester,
      Integer currentSemester,
      Map<Long, List<PrerequisiteView>> prerequisites) {
    int absolute = absoluteIndex(semester.getYear(), semester.getSemester());
    String timing = timing(absolute, currentSemester, "FUTURE");
    List<CourseView> courses =
        semester.getItems().stream()
            .map(
                item ->
                    new CourseView(
                        item.getSubject().getCode(),
                        item.getSubject().getName(),
                        timing,
                        false,
                        item.getScore(),
                        prerequisites.getOrDefault(item.getSubject().getId(), List.of())))
            .toList();
    return new SemesterView(
        semester.getYear(), semester.getSemester(), semester.getStage().name(), timing, courses);
  }

  private List<PastGroup> groupCompletedCourses(OnboardingProfileSnapshot profile) {
    Map<String, PastGroup> grouped = new LinkedHashMap<>();
    for (CompletedCourse course : profile.completedCourses()) {
      Subject subject = subjectRepository.findByCode(course.subjectCode()).orElse(null);
      if (subject == null) {
        continue;
      }
      String key = course.year() + "-" + course.semester();
      grouped
          .computeIfAbsent(key, k -> new PastGroup(course.year(), course.semester()))
          .subjects()
          .add(subject);
    }
    return grouped.values().stream()
        .sorted(Comparator.comparingInt(PastGroup::absoluteIndex))
        .toList();
  }

  private Map<Long, List<PrerequisiteView>> prerequisiteIndex(List<Subject> subjects) {
    Map<Long, List<PrerequisiteView>> index = new LinkedHashMap<>();
    if (subjects.isEmpty()) {
      return index;
    }
    for (SubjectPrerequisite link : subjectPrerequisiteRepository.findBySubjectIn(subjects)) {
      Subject prerequisite = link.getPrerequisite();
      index
          .computeIfAbsent(link.getSubject().getId(), k -> new ArrayList<>())
          .add(new PrerequisiteView(prerequisite.getCode(), prerequisite.getName()));
    }
    return index;
  }

  private Map<Long, SubjectStage> pastStageIndex(List<PastGroup> pastGroups) {
    List<Subject> subjects = new ArrayList<>();
    pastGroups.forEach(g -> subjects.addAll(g.subjects()));
    Map<Long, SubjectStage> index = new LinkedHashMap<>();
    if (subjects.isEmpty()) {
      return index;
    }
    for (TrackSubject link : trackSubjectRepository.findBySubjectIn(subjects)) {
      index.putIfAbsent(link.getSubject().getId(), link.getStage());
    }
    return index;
  }

  private String timing(int absoluteIndex, Integer currentSemester, String fallback) {
    if (currentSemester == null) {
      return fallback;
    }
    if (absoluteIndex < currentSemester) {
      return "PAST";
    }
    return absoluteIndex == currentSemester ? "CURRENT" : "FUTURE";
  }

  private static SubjectStage stageForYear(int year) {
    int clamped = Math.min(Math.max(year, 1), STAGE_BY_YEAR.length);
    return STAGE_BY_YEAR[clamped - 1];
  }

  private static int absoluteIndex(int year, int semester) {
    return (year - 1) * 2 + semester;
  }

  /** 같은 (학년, 학기)로 묶인 이수 과목 그룹 — 과거 학기 재구성 단위. */
  private record PastGroup(int year, int semester, List<Subject> subjects) {
    PastGroup(int year, int semester) {
      this(year, semester, new ArrayList<>());
    }

    int absoluteIndex() {
      return RecommendationAssembler.absoluteIndex(year, semester);
    }
  }
}
