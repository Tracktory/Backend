package com.hansung.tracktory.global.response;

import java.util.List;

public record ErrorPayload(
        String code,
        String message,
        List<FieldError> details
) {
    public static ErrorPayload of(String code, String message) {
        return new ErrorPayload(code, message, null);
    }

    public static ErrorPayload of(String code, String message, List<FieldError> details) {
        return new ErrorPayload(code, message, details);
    }
}
