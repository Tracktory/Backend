package com.hansung.tracktory.domain.chatbot.ai;

import com.hansung.tracktory.domain.recommendation.ai.AiEnvelope;
import com.hansung.tracktory.global.exception.BusinessException;
import com.hansung.tracktory.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * AI 중계 서버(FastAPI)의 챗봇 엔드포인트를 호출하는 클라이언트.
 *
 * <p>내부 인증 헤더(X-Internal-Token)·사용자 식별 헤더(X-User-Id)는 {@code aiRelayWebClient} 빈의 필터가 자동 부착하므로 여기서
 * 설정하지 않는다. 필터가 호출 스레드의 SecurityContext 를 읽으므로 인증된 요청 스레드에서 블로킹 호출해야 한다.
 */
@Component
public class ChatbotClient {

  private static final String CHATBOT_PATH = "/api/v1/ai/chat";
  private static final Logger log = LoggerFactory.getLogger(ChatbotClient.class);

  private final WebClient aiRelayWebClient;

  public ChatbotClient(@Qualifier("aiRelayWebClient") WebClient aiRelayWebClient) {
    this.aiRelayWebClient = aiRelayWebClient;
  }

  // 챗봇 메시지를 FastAPI 로 중계하고 응답을 반환 (실패 시 AI_RELAY_ERROR)
  public ChatbotAiResponse send(ChatbotAiRequest request) {
    AiEnvelope<ChatbotAiResponse> envelope;
    try {
      envelope =
          aiRelayWebClient
              .post()
              .uri(CHATBOT_PATH)
              .bodyValue(request)
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<AiEnvelope<ChatbotAiResponse>>() {})
              .block();
    } catch (WebClientResponseException e) {
      log.warn("AI relay 응답 오류: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BusinessException(ErrorCode.AI_RELAY_ERROR);
    } catch (WebClientException e) {
      log.warn("AI relay 호출 실패", e);
      throw new BusinessException(ErrorCode.AI_RELAY_ERROR);
    }

    if (envelope == null || !envelope.success() || envelope.data() == null) {
      throw new BusinessException(ErrorCode.AI_RELAY_ERROR);
    }
    return envelope.data();
  }
}
