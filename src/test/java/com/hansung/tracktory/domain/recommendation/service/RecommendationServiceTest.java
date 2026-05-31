package com.hansung.tracktory.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hansung.tracktory.domain.catalog.career.entity.Job;
import com.hansung.tracktory.domain.catalog.career.repository.JobRepository;
import com.hansung.tracktory.domain.catalog.curriculum.entity.Subject;
import com.hansung.tracktory.domain.catalog.curriculum.entity.SubjectStage;
import com.hansung.tracktory.domain.catalog.curriculum.repository.SubjectRepository;
import com.hansung.tracktory.domain.catalog.organization.entity.Track;
import com.hansung.tracktory.domain.catalog.organization.repository.TrackRepository;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendClient;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.Explanation;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.ExplanationSection;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.JobCandidate;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RankedCombo;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.Roadmap;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapCourse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapStage;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.SemesterPlan;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.TrackCombo;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.entity.Recommendation;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.entity.RecommendedTrack;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileReader;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot;
import com.hansung.tracktory.domain.recommendation.onboarding.OnboardingProfileSnapshot.CompletedCourse;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

  private static final long USER_ID = 1L;

  @InjectMocks private RecommendationService recommendationService;

  @Mock private OnboardingProfileReader onboardingProfileReader;
  @Mock private AiRecommendClient aiRecommendClient;
  @Mock private RecommendationAssembler recommendationAssembler;
  @Mock private UserRepository userRepository;
  @Mock private RecommendationRepository recommendationRepository;
  @Mock private JobRepository jobRepository;
  @Mock private TrackRepository trackRepository;
  @Mock private SubjectRepository subjectRepository;

  @Test
  void generate_onboardingMissing_throwsOnboardingNotFound() {
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> recommendationService.generate(USER_ID, false))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e ->
                assertThat(((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_NOT_FOUND));
    verify(aiRecommendClient, never()).generate(any());
  }

  @Test
  void generate_existingActiveAndNotForce_reusesWithoutAiCall() {
    OnboardingProfileSnapshot profile = sampleProfile();
    Recommendation existing = Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    RecommendationResponse sentinel = sentinelResponse();
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.of(existing));
    given(recommendationAssembler.assemble(existing, profile)).willReturn(sentinel);

    RecommendationResponse result = recommendationService.generate(USER_ID, false);

    assertThat(result).isSameAs(sentinel);
    verify(aiRecommendClient, never()).generate(any());
    verify(recommendationRepository, never()).save(any());
  }

  @Test
  void generate_freshWhenNoActive_callsAiSupersedesSavesAndMaps() {
    OnboardingProfileSnapshot profile = sampleProfile();
    Recommendation previousActive =
        Recommendation.builder().status(RecommendationStatus.ACTIVE).build();
    RecommendationResponse sentinel = sentinelResponse();

    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.empty());
    given(aiRecommendClient.generate(any())).willReturn(sampleAiResponse());
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));
    given(recommendationRepository.findByUser_IdAndStatus(USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(List.of(previousActive));
    given(jobRepository.findByCode("be_dev"))
        .willReturn(Optional.of(Job.builder().code("be_dev").name("백엔드 개발자").build()));
    given(trackRepository.findByCode("BIGDATA"))
        .willReturn(Optional.of(Track.builder().code("BIGDATA").name("빅데이터트랙").build()));
    given(trackRepository.findByCode("WEB"))
        .willReturn(Optional.of(Track.builder().code("WEB").name("웹공학트랙").build()));
    given(trackRepository.findByCode("MOBILE"))
        .willReturn(Optional.of(Track.builder().code("MOBILE").name("모바일소프트웨어트랙").build()));
    given(subjectRepository.findByCode("W080001"))
        .willReturn(Optional.of(Subject.builder().code("W080001").name("프로그래밍기초").build()));
    given(recommendationRepository.save(any(Recommendation.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(recommendationAssembler.assemble(any(Recommendation.class), eq(profile)))
        .willReturn(sentinel);

    RecommendationResponse result = recommendationService.generate(USER_ID, false);

    assertThat(result).isSameAs(sentinel);
    assertThat(previousActive.getStatus()).isEqualTo(RecommendationStatus.SUPERSEDED);

    ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
    verify(recommendationRepository).save(captor.capture());
    Recommendation saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(RecommendationStatus.ACTIVE);
    assertThat(saved.getTrackCombinationScore()).isEqualTo(85);
    assertThat(saved.getTrackCombinationSummary()).isEqualTo("빅데이터트랙 + 웹공학트랙");
    assertThat(saved.getTrackCombinationReasoning()).isEqualTo("트랙 설명");

    assertThat(saved.getRecommendedJobs()).hasSize(1);
    assertThat(saved.getRecommendedJobs().get(0).getScore()).isEqualTo(90);
    assertThat(saved.getRecommendedJobs().get(0).getJob().getCode()).isEqualTo("be_dev");

    assertThat(saved.getRecommendedTracks()).hasSize(3);
    List<RecommendedTrack> primaries =
        saved.getRecommendedTracks().stream().filter(RecommendedTrack::isPrimary).toList();
    List<RecommendedTrack> secondaries =
        saved.getRecommendedTracks().stream().filter(t -> !t.isPrimary()).toList();
    assertThat(primaries).extracting(t -> t.getTrack().getCode()).containsExactly("BIGDATA", "WEB");
    assertThat(secondaries).extracting(t -> t.getTrack().getCode()).containsExactly("MOBILE");

    assertThat(saved.getRoadmap()).isNotNull();
    assertThat(saved.getRoadmap().getSemesters()).hasSize(1);
    var semester = saved.getRoadmap().getSemesters().get(0);
    assertThat(semester.getYear()).isEqualTo(3);
    assertThat(semester.getSemester()).isEqualTo(1);
    assertThat(semester.getStage()).isEqualTo(SubjectStage.FOUNDATION);
    assertThat(semester.getItems()).hasSize(1);
    assertThat(semester.getItems().get(0).getSubject().getCode()).isEqualTo("W080001");
    assertThat(semester.getItems().get(0).getScore()).isEqualTo(60);
  }

  @Test
  void generate_forceRefresh_bypassesReuseAndCallsAi() {
    OnboardingProfileSnapshot profile = sampleProfile();
    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(aiRecommendClient.generate(any())).willReturn(sampleAiResponse());
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));
    given(recommendationRepository.findByUser_IdAndStatus(USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(List.of());
    given(jobRepository.findByCode("be_dev"))
        .willReturn(Optional.of(Job.builder().code("be_dev").name("백엔드 개발자").build()));
    given(trackRepository.findByCode("BIGDATA"))
        .willReturn(Optional.of(Track.builder().code("BIGDATA").name("빅데이터트랙").build()));
    given(trackRepository.findByCode("WEB"))
        .willReturn(Optional.of(Track.builder().code("WEB").name("웹공학트랙").build()));
    given(trackRepository.findByCode("MOBILE"))
        .willReturn(Optional.of(Track.builder().code("MOBILE").name("모바일소프트웨어트랙").build()));
    given(subjectRepository.findByCode("W080001"))
        .willReturn(Optional.of(Subject.builder().code("W080001").name("프로그래밍기초").build()));
    given(recommendationRepository.save(any(Recommendation.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(recommendationAssembler.assemble(any(Recommendation.class), eq(profile)))
        .willReturn(sentinelResponse());

    recommendationService.generate(USER_ID, true);

    verify(aiRecommendClient).generate(any());
    verify(recommendationRepository, never())
        .findFirstByUser_IdAndStatusOrderByCreatedAtDesc(any(), any());
  }

  @Test
  void generate_jobsFoldingToSameCatalogCode_dedupedToSingleRecommendedJob() {
    OnboardingProfileSnapshot profile = sampleProfile();
    // AI 서버가 세분화 직무를 같은 카탈로그 코드(DE)로 fold 한 응답 — (recommendation_id, job_id) 유니크 제약 회귀 가드.
    AiRecommendResponse ai =
        new AiRecommendResponse(
            List.of(
                new JobCandidate("DE", "데이터 엔지니어", List.of(), List.of(), 0.9, 0.8, false),
                new JobCandidate("DE", "데이터 분석가", List.of(), List.of(), 0.5, 0.4, false)),
            List.of(),
            List.of(),
            null,
            null);

    given(onboardingProfileReader.read(USER_ID)).willReturn(Optional.of(profile));
    given(
            recommendationRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(
                USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(Optional.empty());
    given(aiRecommendClient.generate(any())).willReturn(ai);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(sampleUser()));
    given(recommendationRepository.findByUser_IdAndStatus(USER_ID, RecommendationStatus.ACTIVE))
        .willReturn(List.of());
    given(jobRepository.findByCode("DE"))
        .willReturn(Optional.of(Job.builder().code("DE").name("데이터 엔지니어").build()));
    given(recommendationRepository.save(any(Recommendation.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(recommendationAssembler.assemble(any(Recommendation.class), eq(profile)))
        .willReturn(sentinelResponse());

    recommendationService.generate(USER_ID, false);

    ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
    verify(recommendationRepository).save(captor.capture());
    Recommendation saved = captor.getValue();

    assertThat(saved.getRecommendedJobs()).hasSize(1);
    assertThat(saved.getRecommendedJobs().get(0).getJob().getCode()).isEqualTo("DE");
    assertThat(saved.getRecommendedJobs().get(0).getScore()).isEqualTo(90);
  }

  private static OnboardingProfileSnapshot sampleProfile() {
    return new OnboardingProfileSnapshot(
        USER_ID,
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
        List.of(new CompletedCourse("W080001", 1, 1)));
  }

  private static User sampleUser() {
    return User.builder().email("a@b.com").passwordHash("hash").build();
  }

  private static RecommendationResponse sentinelResponse() {
    return new RecommendationResponse(99L, List.of(), null, null);
  }

  private static AiRecommendResponse sampleAiResponse() {
    AiRecommendResponse.Track bigdata =
        new AiRecommendResponse.Track("c", "d", "BIGDATA", "빅데이터트랙");
    AiRecommendResponse.Track web = new AiRecommendResponse.Track("c", "d", "WEB", "웹공학트랙");
    AiRecommendResponse.Track mobile =
        new AiRecommendResponse.Track("c", "d", "MOBILE", "모바일소프트웨어트랙");

    RankedCombo primary =
        new RankedCombo(new TrackCombo(bigdata, web, "BIGDATA+WEB"), 0.85, "primary", 1);
    RankedCombo secondary =
        new RankedCombo(new TrackCombo(mobile, bigdata, "MOBILE+BIGDATA"), 0.6, "cross_college", 2);

    RoadmapCourse course = new RoadmapCourse("W080001", "프로그래밍기초", 0.6, 3, "foundation");
    RoadmapStage stage = new RoadmapStage("foundation", List.of(course));
    SemesterPlan plan = new SemesterPlan(5, 3, List.of(course), 3, false, false);
    Roadmap roadmap = new Roadmap(List.of(stage), List.of(plan), "BIGDATA+WEB");

    Explanation explanation =
        new Explanation(
            "전체 설명",
            List.of(
                new ExplanationSection("tracks", "트랙 설명"),
                new ExplanationSection("roadmap", "로드맵 설명")),
            List.of(),
            List.of());

    return new AiRecommendResponse(
        List.of(new JobCandidate("be_dev", "백엔드 개발자", List.of(), List.of(), 0.9, 0.8, false)),
        List.of(primary),
        List.of(secondary),
        roadmap,
        explanation);
  }
}
