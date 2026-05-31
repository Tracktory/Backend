package com.hansung.tracktory.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.user.service.UserPrincipal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class AiRelayWebClientTest {

  private static final AiRelayProperties PROPS =
      new AiRelayProperties("http://localhost:8000", "test-internal-token");

  private final WebClientConfig config = new WebClientConfig();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 내부토큰_헤더_자동부착() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();
    WebClient client = clientCapturing(captured);

    client.get().uri("/ai/recommend").retrieve().toBodilessEntity().block();

    assertThat(captured.get().headers().getFirst(WebClientConfig.INTERNAL_TOKEN_HEADER))
        .isEqualTo("test-internal-token");
  }

  @Test
  void 인증된사용자_userId_헤더_부착() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();
    WebClient client = clientCapturing(captured);
    UserPrincipal principal = UserPrincipal.of(42L, "x@y.com");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

    client.get().uri("/ai/recommend").retrieve().toBodilessEntity().block();

    assertThat(captured.get().headers().getFirst(WebClientConfig.USER_ID_HEADER)).isEqualTo("42");
  }

  @Test
  void 비인증_userId_헤더_없음() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();
    WebClient client = clientCapturing(captured);

    client.get().uri("/ai/recommend").retrieve().toBodilessEntity().block();

    assertThat(captured.get().headers().getFirst(WebClientConfig.USER_ID_HEADER)).isNull();
  }

  private WebClient clientCapturing(AtomicReference<ClientRequest> captured) {
    ExchangeFunction capturingExchange =
        request -> {
          captured.set(request);
          return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };
    return config.aiRelayWebClientBuilder(PROPS).exchangeFunction(capturingExchange).build();
  }
}
