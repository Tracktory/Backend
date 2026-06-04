package com.hansung.tracktory.domain.briefing.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse.Briefing;
import com.hansung.tracktory.domain.briefing.ai.AiBriefingResponse.Source;
import com.hansung.tracktory.domain.recommendation.ai.AiEnvelope;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 중계 서버(FastAPI) 브리핑 응답 계약의 역직렬화 회귀 가드.
 *
 * <p>실 FastAPI 페이로드 형태({@code {success, data:{briefings:[...]}}} envelope + 카드의
 * job_id/headline/summary + 출처의 published_at)가 DTO 로 손실 없이 매핑되는지 검증한다. WebClient 도 DTO 자체 어노테이션만으로
 * 동일하게 매핑하므로 기본 매퍼로 충분하다.
 */
class AiBriefingEnvelopeContractTest {

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  @Test
  void deserializesRealFastApiBriefingEnvelopeWithoutFieldLoss() {
    String json =
        """
        {
          "success": true,
          "data": {
            "briefings": [
              {
                "job_id": "BE",
                "job_name": "백엔드 개발자",
                "headline": "클라우드 네이티브 백엔드 수요 증가",
                "summary": "컨테이너·MSA 역량이 채용에서 점점 더 중요해지고 있다.",
                "skills": ["Spring Boot", "Kubernetes", "AWS"],
                "sources": [
                  {"title": "Stack Overflow Developer Survey 2024",
                   "url": "https://survey.stackoverflow.co/2024",
                   "published_at": "2024"},
                  {"title": "JetBrains State of Developer Ecosystem",
                   "url": "https://www.jetbrains.com/lp/devecosystem-2023",
                   "published_at": null}
                ]
              }
            ]
          },
          "error": null
        }
        """;

    AiEnvelope<AiBriefingResponse> envelope =
        MAPPER.readValue(json, new TypeReference<AiEnvelope<AiBriefingResponse>>() {});

    assertThat(envelope.success()).isTrue();
    AiBriefingResponse data = envelope.data();
    assertThat(data).isNotNull();
    assertThat(data.briefings()).hasSize(1);

    Briefing card = data.briefings().get(0);
    assertThat(card.jobId()).isEqualTo("BE");
    assertThat(card.jobName()).isEqualTo("백엔드 개발자");
    assertThat(card.headline()).isEqualTo("클라우드 네이티브 백엔드 수요 증가");
    assertThat(card.summary()).isNotBlank();
    assertThat(card.skills()).containsExactly("Spring Boot", "Kubernetes", "AWS");
    assertThat(card.sources()).hasSize(2);

    Source dated = card.sources().get(0);
    assertThat(dated.title()).isEqualTo("Stack Overflow Developer Survey 2024");
    assertThat(dated.url()).isEqualTo("https://survey.stackoverflow.co/2024");
    assertThat(dated.publishedAt()).isEqualTo("2024");

    // published_at 미상(null)도 손실 없이 매핑되어야 한다.
    assertThat(card.sources().get(1).publishedAt()).isNull();
  }
}
