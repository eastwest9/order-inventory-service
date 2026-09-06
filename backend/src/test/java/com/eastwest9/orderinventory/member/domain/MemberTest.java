package com.eastwest9.orderinventory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MemberTest {

    @Test
    void 회원을_기본_USER_권한으로_생성한다() {
        Member member = new Member("member@example.com", "test-password", "테스트 회원");

        assertThat(member.getEmail()).isEqualTo("member@example.com");
        assertThat(member.getPassword()).isEqualTo("test-password");
        assertThat(member.getName()).isEqualTo("테스트 회원");
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @Test
    void 소셜_회원은_비밀번호_없이_생성할_수_있다() {
        Member member = new Member("social@example.com", null, "소셜 회원");

        assertThat(member.getPassword()).isNull();
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 이메일은_필수다(String email) {
        assertThatThrownBy(() -> new Member(email, null, "테스트 회원"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("이메일은 필수입니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 회원명은_필수다(String name) {
        assertThatThrownBy(() -> new Member("member@example.com", null, name))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("회원명은 필수입니다.");
    }

    @Test
    void 필드_최대_길이를_초과할_수_없다() {
        assertThatThrownBy(() -> new Member("a".repeat(255), null, "회원"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("254자");
        assertThatThrownBy(() -> new Member("member@example.com", "a".repeat(256), "회원"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("255자");
        assertThatThrownBy(() -> new Member("member@example.com", null, "가".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("100자");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void 비밀번호가_있으면_공백일_수_없다(String password) {
        assertThatThrownBy(() -> new Member("member@example.com", password, "회원"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("공백");
    }
}
