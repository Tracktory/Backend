package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.entity.JobTechStack;
import com.hansung.tracktory.domain.catalog.career.entity.TechStack;
import com.hansung.tracktory.domain.catalog.career.repository.JobTechStackRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectPrerequisiteRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.curriculum.repository.TrackSubjectRepository;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.JobView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.SemesterView;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedJob;
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
    assertThat(view.score()).isEqualTo(90);
    assertThat(view.techStacks()).containsExactly("Airflow", "Spark");
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
}
