package com.hansung.tracktory.global.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.hansung.tracktory.global.constant.StaticValue.CREATED;
import static com.hansung.tracktory.global.constant.StaticValue.OK;

@Getter
@AllArgsConstructor
public enum SuccessResponseCode implements BaseResponseCode {
    SUCCESS_OK("GLOBAL_200", OK, "호출에 성공했습니다."),
    SUCCESS_CREATED("GLOBAL_201", CREATED, "호출에 성공했습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
