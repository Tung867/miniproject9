package com.spaceres.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ── 회원가입 요청 ──────────────────────────────────────────
@Getter
@NoArgsConstructor
public class SignUpRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일을 입력해주세요")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "이름을 입력해주세요")
    @Size(max = 50)
    private String name;

    @Size(max = 100)
    private String organization;
}
