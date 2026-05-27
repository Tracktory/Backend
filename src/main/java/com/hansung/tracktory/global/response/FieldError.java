package com.hansung.tracktory.global.response;

public record FieldError(
        String field,
        String reason
) {
}
