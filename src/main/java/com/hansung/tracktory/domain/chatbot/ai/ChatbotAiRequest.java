package com.hansung.tracktory.domain.chatbot.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

// FastAPI 챗봇 요청 바디 (camelCase → snake_case, 빈 선택항목 누락)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatbotAiRequest(String threadId, String message, UserContext userContext) {

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public record UserContext(
      Long userId,
      String name,
      int entryYear,
      int grade,
      String college,
      String department,
      List<String> tracks,
      List<String> interests,
      List<String> studyFields,
      List<String> techStacks,
      List<String> companyTypes,
      List<String> workValues,
      List<CompletedSubject> completedSubjects) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompletedSubject(String name, int year, int semester) {}
  }
}
