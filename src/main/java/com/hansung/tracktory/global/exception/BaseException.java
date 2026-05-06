package com.hansung.tracktory.global.exception;

import com.hansung.tracktory.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BaseException extends RuntimeException {
    private final BaseResponseCode baseResponseCode;
}
