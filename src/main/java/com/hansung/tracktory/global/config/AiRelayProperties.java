package com.hansung.tracktory.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ai-relay")
public record AiRelayProperties(@NotBlank String baseUrl, @NotBlank String internalToken) {}
