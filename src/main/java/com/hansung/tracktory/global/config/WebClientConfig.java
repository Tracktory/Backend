package com.hansung.tracktory.global.config;

import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(AiRelayProperties.class)
public class WebClientConfig {

  public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
  public static final String USER_ID_HEADER = "X-User-Id";

  @Bean
  public WebClient webClient() {
    return WebClient.builder().filter(propagateRequestId()).build();
  }

  @Bean
  public WebClient aiRelayWebClient(AiRelayProperties props) {
    return aiRelayWebClientBuilder(props).build();
  }

  // AI 중계 클라이언트 공용 설정. 필터는 요청 조립 시점에 호출 스레드의 컨텍스트(MDC / SecurityContext)를
  // 읽으므로, 블로킹 호출자는 인증 컨텍스트를 가진 동일 스레드에서 요청을 실행해야 한다.
  WebClient.Builder aiRelayWebClientBuilder(AiRelayProperties props) {
    return WebClient.builder()
        .baseUrl(props.baseUrl())
        .defaultHeader(INTERNAL_TOKEN_HEADER, props.internalToken())
        .filter(propagateRequestId())
        .filter(propagateUserId());
  }

  private ExchangeFilterFunction propagateRequestId() {
    return (request, next) -> {
      String requestId = MDC.get(RequestIdFilter.MDC_KEY);
      if (requestId == null || requestId.isBlank()) {
        return next.exchange(request);
      }
      ClientRequest mutated =
          ClientRequest.from(request).header(RequestIdFilter.HEADER_NAME, requestId).build();
      return next.exchange(mutated);
    };
  }

  private ExchangeFilterFunction propagateUserId() {
    return (request, next) -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null
          || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
        return next.exchange(request);
      }
      ClientRequest mutated =
          ClientRequest.from(request)
              .header(USER_ID_HEADER, String.valueOf(userPrincipal.getUserId()))
              .build();
      return next.exchange(mutated);
    };
  }
}
