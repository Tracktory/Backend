package com.hansung.tracktory.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatMessageRequest {

  @NotBlank private String threadId; // 대화 세션 ID (후속 메시지는 재사용)

  @NotBlank @Size(min = 1, max = 2000) private String message; // 사용자 질문 본문
}
