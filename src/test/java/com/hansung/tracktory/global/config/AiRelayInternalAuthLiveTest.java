package com.hansung.tracktory.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 실제 소켓을 통한 라이브 왕복 검증: 운영 AI 중계 wiring을 인-프로세스 스텁 서버로 향하게 해, 내부 토큰 + 사용자 식별 헤더가 (모킹된 exchange
 * function이 아니라) 실제로 네트워크로 전송되는지 확인한다. 데이터베이스 · 환경 변수 · 외부 중계 서버가 필요 없다.
 */
class AiRelayInternalAuthLiveTest {

  private static final String TOKEN = "live-test-internal-token";

  private final WebClientConfig config = new WebClientConfig();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 실제_HTTP_호출에_내부토큰과_userId_헤더가_전송된다() throws IOException {
    Map<String, String> received = new ConcurrentHashMap<>();
    HttpServer stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    stub.createContext(
        "/",
        exchange -> {
          exchange
              .getRequestHeaders()
              .forEach((k, v) -> received.put(k.toLowerCase(Locale.ROOT), String.join(",", v)));
          byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    stub.start();

    try {
      int port = stub.getAddress().getPort();
      AiRelayProperties props = new AiRelayProperties("http://127.0.0.1:" + port, TOKEN);
      WebClient client = config.aiRelayWebClientBuilder(props).build();

      UserPrincipal principal = UserPrincipal.of(7L, "live@test.com");
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

      client.get().uri("/ai/recommend").retrieve().toBodilessEntity().block();

      System.out.println(
          ">>> stub received X-Internal-Token = " + received.get("x-internal-token"));
      System.out.println(">>> stub received X-User-Id        = " + received.get("x-user-id"));

      assertThat(received.get("x-internal-token")).isEqualTo(TOKEN);
      assertThat(received.get("x-user-id")).isEqualTo("7");
    } finally {
      stub.stop(0);
    }
  }
}
