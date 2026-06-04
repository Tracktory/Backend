package com.hansung.tracktory.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.JobView;
import com.hansung.tracktory.domain.recommendation.dto.RecommendationResponse.TrackView;
import com.hansung.tracktory.domain.recommendation.entity.RecommendationStatus;
import com.hansung.tracktory.domain.recommendation.repository.RecommendationRepository;
import com.hansung.tracktory.domain.user.entity.User;
import com.hansung.tracktory.domain.user.repository.UserRepository;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.jwt.JwtUtil;
import com.hansung.tracktory.global.response.ApiResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * 추천 근거 항목별 매핑 e2e — 실제 AI 중계 서버(tracktory-ai feat/175, 항목별 근거 제공)를 호출해 직무·트랙 항목마다 서로 다른 근거가 응답에
 * 바인딩되는지 검증한다.
 *
 * <p>실제 외부 의존성(로컬 AI 서버 + LLM 호출)을 사용하므로 {@code @Tag("e2e")} 로 분리하고, AI 서버가 떠 있지 않으면 {@code
 * assumeTrue} 로 스킵한다(CI 처럼 AI 서버가 없는 환경에서는 실패가 아니라 스킵). 로컬에서 {@code AI_E2E_PORT} 포트(기본 8001)에
 * feat/175 서버가 떠 있을 때만 실행된다. {@code webEnvironment=RANDOM_PORT} 라 데이터가 실제 커밋되므로 생성물은
 * {@code @AfterEach} 에서 정리한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("e2e")
class RecommendationReasoningE2ETest {

  private static final int AI_PORT =
      Integer.parseInt(System.getenv().getOrDefault("AI_E2E_PORT", "8001"));

  @DynamicPropertySource
  static void aiRelayBaseUrl(DynamicPropertyRegistry registry) {
    registry.add("ai-relay.base-url", () -> "http://localhost:" + AI_PORT);
  }

  @LocalServerPort private int port;

  @Autowired private UserRepository userRepository;
  @Autowired private RecommendationRepository recommendationRepository;
  @Autowired private JwtUtil jwtUtil;

  private Long createdUserId;

  @AfterEach
  void cleanup() {
    if (createdUserId == null) {
      return;
    }
    try {
      recommendationRepository.deleteAll(
          recommendationRepository.findByUser_IdAndStatus(
              createdUserId, RecommendationStatus.ACTIVE));
      userRepository.deleteById(createdUserId);
    } catch (RuntimeException ignored) {
      // 정리 실패가 검증 결과를 가리지 않도록 best-effort 로 둔다(개발 DB 한정 e2e).
    }
  }

  @Test
  void recommend_bindsDistinctReasoningPerJobAndTrack() {
    assumeTrue(aiServerUp(), "AI feat/175 서버가 :" + AI_PORT + " 에 없음 — e2e 스킵");

    String email = "e2e-reasoning-" + java.util.UUID.randomUUID() + "@hansung.ac.kr";
    User user = userRepository.save(User.builder().email(email).passwordHash("x").build());
    createdUserId = user.getId();
    String token = jwtUtil.generateToken(new UserPrincipal(user));

    // 추천 파이프라인은 RAGFlow + 항목별 근거 생성 LLM 호출로 길어질 수 있어 읽기 타임아웃을 넉넉히 둔다(백엔드는 자체 타임아웃 없음).
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    requestFactory.setReadTimeout(Duration.ofMinutes(5));
    ResponseEntity<ApiResponse<RecommendationResponse>> response =
        RestClient.builder()
            .requestFactory(requestFactory)
            .build()
            .post()
            .uri("http://localhost:" + port + "/api/v1/recommendations?forceRefresh=true")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {})
            .toEntity(new ParameterizedTypeReference<ApiResponse<RecommendationResponse>>() {});

    // 실 LLM 호출은 OpenAI 지연으로 일시 실패(AI_RELAY_ERROR)할 수 있다. 이는 백엔드 결함이 아니라 외부 의존성
    // 트랜션트이므로, 비정상 응답이면 실패가 아니라 스킵한다(매핑 로직 검증은 결정론 단위/계약 테스트가 담당).
    assumeTrue(
        response.getStatusCode().is2xxSuccessful(),
        "AI 응답 비정상(LLM 지연/일시 오류) — e2e 스킵: " + response.getBody());
    RecommendationResponse body = response.getBody().data();
    assertThat(body).isNotNull();

    // 직무: AI 가 직무 식별자(job_id)별 개별 근거를 제공하므로, 서로 다른 직무는 서로 다른 근거로 바인딩된다.
    List<JobView> jobs = body.jobs();
    assertThat(jobs).as("추천 직무").hasSizeGreaterThanOrEqualTo(2);
    assertThat(jobs).allSatisfy(j -> assertThat(j.reasoning()).isNotBlank());
    assertThat(jobs.stream().map(JobView::reasoning).toList())
        .as("직무 항목별 근거가 더 이상 동일 문구로 중복되지 않음")
        .doesNotHaveDuplicates();

    // 트랙: 모든 항목에 근거가 채워져 노출된다(항목별 근거 또는 영역 단락 안전 폴백). 조합 전체 근거도 노출된다.
    // 트랙 항목별 문구의 상이성은 AI(LLM) 의 항목별 생성 품질에 의존하므로, 결정론 단위 테스트에서 검증한다.
    List<TrackView> allTracks = new ArrayList<>();
    allTracks.addAll(body.tracks().primary());
    allTracks.addAll(body.tracks().secondary());
    assertThat(allTracks).as("추천 트랙").isNotEmpty();
    assertThat(allTracks).allSatisfy(t -> assertThat(t.reasoning()).isNotBlank());
    assertThat(body.tracks().combinationReasoning()).as("조합 전체 근거").isNotBlank();
    // 응답 본문은 영속 aggregate 를 조립한 결과이므로, 위 검증으로 매핑이 DB 까지 흐른 것이 확인된다.
    // (영속 엔티티 직접 검증은 결정론 단위 테스트가 ArgumentCaptor 로 담당한다.)
  }

  private static boolean aiServerUp() {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", AI_PORT), 500);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
