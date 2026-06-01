package com.hansung.tracktory.domain.chatbot.dto;

import java.util.List;

public record ChatMessageResponse(String threadId, String message, List<String> choices) {}
