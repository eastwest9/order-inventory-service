package com.eastwest9.orderinventory.member.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eastwest9.orderinventory.common.exception.GlobalExceptionHandler;
import com.eastwest9.orderinventory.member.domain.MemberRole;
import com.eastwest9.orderinventory.member.dto.MemberCreateRequestDto;
import com.eastwest9.orderinventory.member.dto.MemberResponseDto;
import com.eastwest9.orderinventory.member.exception.DuplicateMemberEmailException;
import com.eastwest9.orderinventory.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
@Import(GlobalExceptionHandler.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    void 회원가입_성공시_password_없이_201을_반환한다() throws Exception {
        given(memberService.createMember(any(MemberCreateRequestDto.class)))
                .willReturn(new MemberResponseDto(1L, "user@example.com", "홍길동", MemberRole.USER));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(" Test@Example.COM ", "password123!", "홍길동")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$", not(hasKey("password"))));
    }

    @Test
    void 중복_이메일은_409를_반환한다() throws Exception {
        given(memberService.createMember(any(MemberCreateRequestDto.class)))
                .willThrow(new DuplicateMemberEmailException("user@example.com"));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("user@example.com", "password123!", "홍길동")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다. email=user@example.com"));
    }

    @Test
    void 잘못된_이메일은_400을_반환한다() throws Exception {
        assertInvalidRequest(validRequest("invalid-email", "password123!", "홍길동"), "email: 이메일 형식이 올바르지 않습니다.");
    }

    @Test
    void blank_비밀번호는_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("user@example.com", "", "홍길동")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 짧은_비밀번호는_400을_반환한다() throws Exception {
        assertInvalidRequest(validRequest("user@example.com", "short", "홍길동"), "password: 비밀번호는 8자 이상 255자 이하여야 합니다.");
    }

    @Test
    void blank_회원명은_400을_반환한다() throws Exception {
        assertInvalidRequest(validRequest("user@example.com", "password123!", ""), "name: 회원명은 필수입니다.");
    }

    private void assertInvalidRequest(String request, String message) throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(message));
    }

    private String validRequest(String email, String password, String name) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "name": "%s"
                }
                """.formatted(email, password, name);
    }
}
