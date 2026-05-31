package com.hansung.tracktory.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.hansung.tracktory.domain.chatbot.ai.ChatbotAiResponse;
import com.hansung.tracktory.domain.chatbot.ai.ChatbotClient;
import com.hansung.tracktory.domain.chatbot.dto.ChatMessageRequest;
import com.hansung.tracktory.domain.chatbot.dto.ChatMessageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

  @InjectMocks private ChatbotService service;

  @Mock private ChatbotClient chatbotClient;
  @Mock private ChatbotContextAssembler contextAssembler;

  @Test
  void sendMessage_stripsEvidenceMarker() { // 끝의 [근거: ...] 표기 제거
    given(chatbotClient.send(any()))
        .willReturn(new ChatbotAiResponse("tid", "웹공학트랙을 추천드립니다. [근거: #4, #5]", List.of("q1")));

    ChatMessageResponse result = service.sendMessage(1L, request("tid", "추천해줘"));

    assertThat(result.message()).isEqualTo("웹공학트랙을 추천드립니다.");
  }

  @Test
  void sendMessage_passesThroughThreadIdAndChoices() { // threadId·choices 그대로 전달
    given(chatbotClient.send(any()))
        .willReturn(new ChatbotAiResponse("tid", "본문", List.of("q1", "q2")));

    ChatMessageResponse result = service.sendMessage(1L, request("tid", "추천해줘"));

    assertThat(result.threadId()).isEqualTo("tid");
    assertThat(result.choices()).containsExactly("q1", "q2");
  }

  @Test
  void sendMessage_noEvidence_unchanged() { // 근거 표기 없으면 본문 그대로
    given(chatbotClient.send(any()))
        .willReturn(new ChatbotAiResponse("tid", "근거가 없는 평범한 답변입니다.", List.of()));

    ChatMessageResponse result = service.sendMessage(1L, request("tid", "추천해줘"));

    assertThat(result.message()).isEqualTo("근거가 없는 평범한 답변입니다.");
  }

  // ------------------------------ helpers ------------------------------

  private static ChatMessageRequest request(String threadId, String message) {
    ChatMessageRequest r = new ChatMessageRequest();
    ReflectionTestUtils.setField(r, "threadId", threadId);
    ReflectionTestUtils.setField(r, "message", message);
    return r;
  }
}
