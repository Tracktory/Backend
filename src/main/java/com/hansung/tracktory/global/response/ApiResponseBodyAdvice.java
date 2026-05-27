package com.hansung.tracktory.global.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.hansung.tracktory")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final String BASE_PACKAGE = "com.hansung.tracktory";

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Guard 1 (package): 프로젝트 base package 화이트리스트 (springdoc/Actuator 이중 wrap 방지)
        Class<?> declaringClass = returnType.getDeclaringClass();
        if (!declaringClass.getPackageName().startsWith(BASE_PACKAGE)) {
            return false;
        }
        // Guard 2 (type): 이미 ApiResponse 면 패스
        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // Guard 3 (runtime): JSON 응답에만 wrap 적용 (String/Resource/file 직렬화 사고 방지)
        if (selectedContentType == null
                || !MediaType.APPLICATION_JSON.isCompatibleWith(selectedContentType)) {
            return body;
        }
        if (body instanceof ApiResponse<?>) {
            return body;
        }
        return ApiResponse.ok(body);
    }
}
