package com.eastwest9.orderinventory.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.domain.MemberRole;
import com.eastwest9.orderinventory.member.dto.MemberCreateRequestDto;
import com.eastwest9.orderinventory.member.dto.MemberResponseDto;
import com.eastwest9.orderinventory.member.exception.DuplicateMemberEmailException;
import com.eastwest9.orderinventory.member.repository.MemberRepository;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 이메일을_정규화하고_비밀번호를_암호화해_USER로_가입한다() {
        MemberCreateRequestDto request = new MemberCreateRequestDto(" Test@Example.COM ", "password123!", "홍길동");
        given(memberRepository.existsByEmail("test@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", 1L);
            return member;
        });

        MemberResponseDto response = memberService.createMember(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        Member saved = memberCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(MemberRole.USER);
        assertThat(response).isEqualTo(new MemberResponseDto(1L, "test@example.com", "홍길동", MemberRole.USER));
    }

    @Test
    void 정규화된_이메일이_이미_존재하면_가입을_거부한다() {
        MemberCreateRequestDto request = new MemberCreateRequestDto(" Test@Example.COM ", "password123!", "홍길동");
        given(memberRepository.existsByEmail("test@example.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(DuplicateMemberEmailException.class)
                .hasMessage("이미 사용 중인 이메일입니다. email=test@example.com");

        verify(passwordEncoder, never()).encode(any());
        verify(memberRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이메일_UNIQUE_경합도_회원_이메일_중복으로_변환한다() {
        MemberCreateRequestDto request = new MemberCreateRequestDto("member@example.com", "password123!", "회원");
        given(memberRepository.existsByEmail("member@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(constraintViolation("uk_member_email"));

        assertThatThrownBy(() -> memberService.createMember(request))
                .isInstanceOf(DuplicateMemberEmailException.class)
                .hasMessageContaining("member@example.com");
    }

    @Test
    void 다른_DB_제약_위반은_회원_이메일_중복으로_변환하지_않는다() {
        MemberCreateRequestDto request = new MemberCreateRequestDto("member@example.com", "password123!", "회원");
        DataIntegrityViolationException exception = constraintViolation("another_constraint");
        given(memberRepository.existsByEmail("member@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123!")).willReturn("encoded-password");
        given(memberRepository.saveAndFlush(any(Member.class))).willThrow(exception);

        assertThatThrownBy(() -> memberService.createMember(request)).isSameAs(exception);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException("constraint violation", new SQLException(), "insert", constraintName);
        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}
