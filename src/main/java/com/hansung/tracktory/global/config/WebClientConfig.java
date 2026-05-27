package com.hansung.tracktory.global.config;

import com.hansung.tracktory.global.filter.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(propagateRequestId())
                .build();
    }

    private ExchangeFilterFunction propagateRequestId() {
        return (request, next) -> {
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId == null || requestId.isBlank()) {
                return next.exchange(request);
            }
            ClientRequest mutated = ClientRequest.from(request)
                    .header(RequestIdFilter.HEADER_NAME, requestId)
                    .build();
            return next.exchange(mutated);
        };
    }
}
