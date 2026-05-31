package com.hansung.tracktory.domain.chatbot.controller;

import com.hansung.tracktory.domain.chatbot.dto.ChatMessageRequest;
import com.hansung.tracktory.domain.chatbot.dto.ChatMessageResponse;
import com.hansung.tracktory.domain.chatbot.service.ChatbotService;
import com.hansung.tracktory.domain.user.service.UserPrincipal;
import com.hansung.tracktory.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot/message")
@RequiredArgsConstructor
public class ChatbotController {

  private final ChatbotService chatbotService;

  @PostMapping
  public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ChatMessageRequest request) {
    ChatMessageResponse response = chatbotService.sendMessage(principal.getUserId(), request);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}
