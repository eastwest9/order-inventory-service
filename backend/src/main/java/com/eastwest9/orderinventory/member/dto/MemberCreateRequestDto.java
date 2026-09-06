package com.eastwest9.orderinventory.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateRequestDto(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 254, message = "이메일은 254자를 초과할 수 없습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다.")
        String password,

        @NotBlank(message = "회원명은 필수입니다.")
        @Size(max = 100, message = "회원명은 100자를 초과할 수 없습니다.")
        String name
) {

    public MemberCreateRequestDto {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "MemberCreateRequestDto[email=" + email + ", password=[PROTECTED], name=" + name + "]";
    }
}
