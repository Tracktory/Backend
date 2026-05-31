package com.hansung.tracktory.domain.recommendation.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.JobCandidate;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RankedCombo;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.RoadmapCourse;
import com.hansung.tracktory.domain.recommendation.ai.AiRecommendResponse.SemesterPlan;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 중계 서버(FastAPI) 응답 계약의 역직렬화 회귀 가드.
 *
 * <p>실 FastAPI 페이로드 형태({@code {success, data, error}} envelope + course score/credits/stage + track
 * 추가 필드)가 DTO 로 손실 없이 매핑되는지 검증한다. WebClient 도 DTO 자체 어노테이션(@JsonNaming / @JsonIgnoreProperties)만으로
 * 동일하게 매핑하므로 기본 매퍼로 충분하다.
 */
class AiEnvelopeContractTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void deserializesRealFastApiEnvelopeWithoutFieldLoss() {
    String json =
        """
        {
          "success": true,
          "data": {
            "jobs": [
              {"job_id":"ml_engineer","job_name":"머신러닝 엔지니어","tech_stacks":["Python","PyTorch"],
               "competency_tags":["문제해결"],"match_score":0.91,"similarity":0.88,"fallback_used":false}
            ],
            "primary_combos": [
              {"combo":{
                 "track_a":{"college_id":"IT공과대학","department_id":"컴퓨터공학부","track_id":"빅데이터트랙",
                            "track_name":"빅데이터트랙","major_id":"컴퓨터공학부","course_ids":["V020002"],
                            "meta_text":"...","meta_vector":[0.1,0.2],"competencies":["데이터"],"tech_stacks":["Python"]},
                 "track_b":{"college_id":"미래플러스대학(계약학과)","department_id":"미래플러스대학",
                            "track_id":"AIㆍ소프트웨어학과","track_name":"AIㆍ소프트웨어학과","major_id":"미래플러스대학",
                            "course_ids":["W080001"],"meta_text":"...","meta_vector":[0.3],"competencies":["AI"],"tech_stacks":["PyTorch"]},
                 "combo_key":"빅데이터트랙|AIㆍ소프트웨어학과"},
               "synergy_score":0.0,"slot_type":"primary","rank":1}
            ],
            "secondary_combos": [],
            "roadmap": {
              "stages": [
                {"stage":"foundation","courses":[
                   {"course_id":"V020002","course_name":"프로그래밍기초","credits":3,"stage":"foundation","score":0.6}]}
              ],
              "semesters": [
                {"semester":3,"grade":2,"courses":[
                   {"course_id":"V020002","course_name":"프로그래밍기초","credits":3,"stage":"foundation","score":0.6}],
                 "credits_total":3,"cap_reached":false,"graduation_insufficient":false}
              ],
              "derived_from_combo_key":"빅데이터트랙|AIㆍ소프트웨어학과"
            },
            "explanation": {
              "text":"전체 설명",
              "sections":[{"topic":"tracks","body":"트랙 설명"}],
              "semester_subtitles":[{"semester":3,"subtitle":"기초 다지기"}],
              "course_flows":[{"course_id":"V020002","flow":"기초→응용"}]
            }
          },
          "error": null
        }
        """;

    AiEnvelope<AiRecommendResponse> envelope =
        MAPPER.readValue(json, new TypeReference<AiEnvelope<AiRecommendResponse>>() {});

    assertThat(envelope.success()).isTrue();
    AiRecommendResponse data = envelope.data();
    assertThat(data).isNotNull();

    JobCandidate job = data.jobs().get(0);
    assertThat(job.jobId()).isEqualTo("ml_engineer");
    assertThat(job.matchScore()).isEqualTo(0.91);
    assertThat(job.fallbackUsed()).isFalse();

    RankedCombo primary = data.primaryCombos().get(0);
    assertThat(primary.combo().trackA().trackId()).isEqualTo("빅데이터트랙");
    assertThat(primary.combo().trackB().trackId()).isEqualTo("AIㆍ소프트웨어학과");
    assertThat(primary.synergyScore()).isEqualTo(0.0);

    SemesterPlan plan = data.roadmap().semesters().get(0);
    assertThat(plan.semester()).isEqualTo(3);
    assertThat(plan.creditsTotal()).isEqualTo(3);

    RoadmapCourse course = plan.courses().get(0);
    assertThat(course.courseId()).isEqualTo("V020002");
    assertThat(course.score()).isEqualTo(0.6);
    assertThat(course.credits()).isEqualTo(3);
    assertThat(course.stage()).isEqualTo("foundation");

    assertThat(data.explanation().sections().get(0).topic()).isEqualTo("tracks");
    assertThat(data.explanation().semesterSubtitles().get(0).semester()).isEqualTo(3);
  }
}
