package com.hansung.tracktory.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  AUTH_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
  AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 서버 오류가 발생했습니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "요청 형식이 올바르지 않습니다."),
  ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 온보딩이 완료된 사용자입니다."),
  ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "온보딩 정보를 찾을 수 없습니다. 먼저 온보딩을 완료해주세요."),
  AI_RELAY_ERROR(HttpStatus.BAD_GATEWAY, "추천 생성 중 AI 서버 오류가 발생했습니다.");

  private final HttpStatus httpStatus;
  private final String defaultMessage;
}
