package com.hansung.tracktory.domain.chatbot.service;

import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiRequest;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiRequest.UserContext;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiResponse;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotClient;
import com.hansung.tracktory.domain.chatbot.dto.ChatMessageRequest;
import com.hansung.tracktory.domain.chatbot.dto.ChatMessageResponse;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatbotService {

  // 응답 끝의 "[근거: #4, #5]" 형태 근거 표기 제거
  private static final Pattern EVIDENCE = Pattern.compile("\\s*\\[근거:[^\\]]*\\]");

  private final ChatbotClient chatbotClient;
  private final ChatbotContextAssembler contextAssembler;

  public ChatMessageResponse sendMessage(Long userId, ChatMessageRequest request) {
    UserContext userContext = contextAssembler.assemble(userId);
    ChatbotAiResponse aiResponse =
        chatbotClient.send(
            new ChatbotAiRequest(request.getThreadId(), request.getMessage(), userContext));
    return new ChatMessageResponse(
        aiResponse.threadId(), stripEvidence(aiResponse.message()), aiResponse.choices());
  }

  private String stripEvidence(String message) {
    if (message == null) {
      return null;
    }
    return EVIDENCE.matcher(message).replaceAll("").stripTrailing();
  }
}
