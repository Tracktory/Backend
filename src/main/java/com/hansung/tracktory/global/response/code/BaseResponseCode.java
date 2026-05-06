package com.hansung.tracktory.global.response.code;

public interface BaseResponseCode {
    String getCode();
    int getHttpStatus();
    String getMessage();
}
