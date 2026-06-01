package com.hansung.tracktory.domain.chatbot.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// FastAPI 챗봇 응답 본문 (snake_case → camelCase, 모르는 필드 무시)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatbotAiResponse(String threadId, String message, List<String> choices) {}
