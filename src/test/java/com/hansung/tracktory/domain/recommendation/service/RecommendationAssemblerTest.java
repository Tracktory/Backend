package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTechStack;
import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import com.hansung.tracktory.domain.catalog.career.repository.JobTechStackRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectType;
import com.hansung.tracktory.domain.catalog.curriculum.entity.TrackSubject;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectPrerequisiteRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.JobView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.MainSubjectView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.SemesterView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.TrackView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.entity.Roadmap;
import com.hansung.tracktory.domain.recommendation.entity.RoadmapItem;
import com.hansung.tracktory.domain.recommendation.entity.RoadmapSemester;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationAssemblerTest {

  @InjectMocks private RecommendationAssembler recommendationAssembler;

  @Mock private SubjectRepository subjectRepository;
  @Mock private SubjectPrerequisiteRepository subjectPrerequisiteRepository;
  @Mock private TrackSubjectRepository trackSubjectRepository;
  @Mock private JobTechStackRepository jobTechStackRepository;

  @Test
  void assemble_prependsReconstructedPastSemestersBeforeFutureWithTiming() {
    Subject past1 = subject(1L, "W1", "자료구조");
    Subject past2 = subject(2L, "W2", "알고리즘");
    Subject future = subject(3L, "W3", "캡스톤");

    given(subjectRepository.findByCode("W1")).willReturn(Optional.of(past1));
    given(subjectRepository.findByCode("W2")).willReturn(Optional.of(past2));
    given(subjectPrerequisiteRepository.findBySubjectIn(anyCollection())).willReturn(List.of());
    given(trackSubjectRepository.findBySubjectIn(anyCollection())).willReturn(List.of());

    Recommendation recommendation =
        Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    Roadmap roadmap = Roadmap.builder().reasoning("로드맵 설명").build();
    recommendation.attachRoadmap(roadmap);
    RoadmapSemester futureSemester =
        RoadmapSemester.builder().year(4).semester(1).stage(SubjectStage.APPLIED).build();
    roadmap.addSemester(futureSemester);
    futureSemester.addItem(RoadmapItem.builder().subject(future).score(2).build());

    OnboardingProfileSnapshot profile =
        new OnboardingProfileSnapshot(
            1L,
            2023,
            "IT공과대학",
            "컴퓨터공학부",
            3,
            List.of("BIGDATA", "WEB"),
            List.of("IT/인터넷"),
            List.of("앱"),
            List.of("성장성"),
            List.of("대기업"),
            List.of(),
            List.of(new CompletedCourse("W1", 1, 1), new CompletedCourse("W2", 2, 1)));

    RecommendationResponse response = recommendationAssembler.assemble(recommendation, profile);

    List<SemesterView> semesters = response.roadmap().semesters();
    assertThat(response.roadmap().reasoning()).isEqualTo("로드맵 설명");
    assertThat(semesters).hasSize(3);

    SemesterView first = semesters.get(0);
    assertThat(first.year()).isEqualTo(1);
    assertThat(first.semester()).isEqualTo(1);
    assertThat(first.stage()).isEqualTo(SubjectStage.FOUNDATION.name());
    assertThat(first.timing()).isEqualTo("PAST");
    assertThat(first.courses()).hasSize(1);
    assertThat(first.courses().get(0).code()).isEqualTo("W1");
    assertThat(first.courses().get(0).completed()).isTrue();
    assertThat(first.courses().get(0).score()).isNull();

    SemesterView second = semesters.get(1);
    assertThat(second.year()).isEqualTo(2);
    assertThat(second.stage()).isEqualTo(SubjectStage.CORE.name());
    assertThat(second.timing()).isEqualTo("CURRENT");
    assertThat(second.courses().get(0).code()).isEqualTo("W2");
    assertThat(second.courses().get(0).completed()).isTrue();

    SemesterView third = semesters.get(2);
    assertThat(third.year()).isEqualTo(4);
    assertThat(third.stage()).isEqualTo(SubjectStage.APPLIED.name());
    assertThat(third.timing()).isEqualTo("FUTURE");
    assertThat(third.courses().get(0).code()).isEqualTo("W3");
    assertThat(third.courses().get(0).completed()).isFalse();
    assertThat(third.courses().get(0).score()).isEqualTo(2);
  }

  @Test
  void assemble_populatesJobTechStacksFromCatalogSortedByName() {
    Job job = mock(Job.class);
    given(job.getId()).willReturn(10L);
    given(job.getCode()).willReturn("DE");
    given(job.getName()).willReturn("데이터 엔지니어");

    Recommendation recommendation =
        Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    recommendation.addRecommendedJob(
        RecommendedJob.builder().score(90).reasoning("이유").job(job).build());

    JobTechStack spark = jobTechStack(job, "Spark");
    JobTechStack airflow = jobTechStack(job, "Airflow");
    given(jobTechStackRepository.findByJobIn(anyCollection())).willReturn(List.of(spark, airflow));

    OnboardingProfileSnapshot profile =
        new OnboardingProfileSnapshot(
            1L,
            2023,
            "IT공과대학",
            "컴퓨터공학부",
            3,
            List.of("BIGDATA", "WEB"),
            List.of("IT/인터넷"),
            List.of("앱"),
            List.of("성장성"),
            List.of("대기업"),
            List.of(),
            List.of());

    RecommendationResponse response = recommendationAssembler.assemble(recommendation, profile);

    assertThat(response.jobs()).hasSize(1);
    JobView view = response.jobs().get(0);
    assertThat(view.code()).isEqualTo("DE");
    assertThat(view.name()).isEqualTo("데이터 엔지니어");
    // 내부 저장 점수 90 은 응답 노출 시 체감 척도로 보정된다: round(60 + 90×0.40) = 96.
    assertThat(view.score()).isEqualTo(96);
    assertThat(view.reasoning()).isEqualTo("이유");
    assertThat(view.techStacks()).containsExactly("Airflow", "Spark");
  }

  @Test
  void assemble_populatesMainSubjectsForRecommendedTracksCappedAtThree() {
    Track track = mock(Track.class);
    given(track.getId()).willReturn(20L);
    given(track.getCode()).willReturn("BIGDATA");
    given(track.getName()).willReturn("빅데이터 트랙");

    Recommendation recommendation =
        Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    recommendation.addRecommendedTrack(
        RecommendedTrack.builder().score(95).reasoning("이유").primary(true).track(track).build());

    TrackSubject industry = trackSubject(track, "S4", "캡스톤", SubjectStage.INDUSTRY);
    TrackSubject foundationB = trackSubject(track, "S2", "이산수학", SubjectStage.FOUNDATION);
    TrackSubject foundationA = trackSubject(track, "S1", "프로그래밍기초", SubjectStage.FOUNDATION);
    TrackSubject core = trackSubject(track, "S3", "데이터베이스", SubjectStage.CORE);
    given(trackSubjectRepository.findByTrackInAndType(anyCollection(), eq(SubjectType.REQUIRED)))
        .willReturn(List.of(industry, foundationB, foundationA, core));

    OnboardingProfileSnapshot profile =
        new OnboardingProfileSnapshot(
            1L,
            2023,
            "IT공과대학",
            "컴퓨터공학부",
            3,
            List.of("BIGDATA", "WEB"),
            List.of("IT/인터넷"),
            List.of("앱"),
            List.of("성장성"),
            List.of("대기업"),
            List.of(),
            List.of());

    RecommendationResponse response = recommendationAssembler.assemble(recommendation, profile);

    assertThat(response.tracks().primary()).hasSize(1);
    TrackView view = response.tracks().primary().get(0);
    assertThat(view.code()).isEqualTo("BIGDATA");
    assertThat(view.primary()).isTrue();
    assertThat(view.reasoning()).isEqualTo("이유");
    // 직무 점수 보정(이슈 #45)은 트랙 점수에 적용되지 않는다: 트랙 점수는 원본 그대로 노출된다(보정 시 95→98).
    assertThat(view.score()).isEqualTo(95);

    List<MainSubjectView> mainSubjects = view.mainSubjects();
    assertThat(mainSubjects).hasSize(3);
    assertThat(mainSubjects).extracting(MainSubjectView::code).containsExactly("S1", "S2", "S3");
    assertThat(mainSubjects.get(0).name()).isEqualTo("프로그래밍기초");
    assertThat(mainSubjects.get(2).name()).isEqualTo("데이터베이스");
  }

  private static Subject subject(long id, String code, String name) {
    Subject subject = mock(Subject.class);
    given(subject.getId()).willReturn(id);
    given(subject.getCode()).willReturn(code);
    given(subject.getName()).willReturn(name);
    return subject;
  }

  private static JobTechStack jobTechStack(Job job, String techStackName) {
    TechStack techStack = mock(TechStack.class);
    given(techStack.getName()).willReturn(techStackName);
    JobTechStack link = mock(JobTechStack.class);
    given(link.getJob()).willReturn(job);
    given(link.getTechStack()).willReturn(techStack);
    return link;
  }

  private static TrackSubject trackSubject(
      Track track, String code, String name, SubjectStage stage) {
    Subject subject = mock(Subject.class);
    TrackSubject link = mock(TrackSubject.class);
    // 정렬·cap(3) 결과에 따라 일부 과목의 접근자는 호출되지 않으므로 lenient 처리.
    lenient().when(subject.getCode()).thenReturn(code);
    lenient().when(subject.getName()).thenReturn(name);
    lenient().when(link.getTrack()).thenReturn(track);
    lenient().when(link.getSubject()).thenReturn(subject);
    lenient().when(link.getStage()).thenReturn(stage);
    return link;
  }
}
