package com.spaceres.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    DUPLICATE_EMAIL(400, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN(401, "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(401, "만료된 Refresh Token입니다. 다시 로그인해주세요."),
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),

    // Common
    INVALID_INPUT(400, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
