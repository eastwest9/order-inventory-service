package com.eastwest9.orderinventory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SocialAccountTest {

    @ParameterizedTest
    @EnumSource(SocialProvider.class)
    void 공급자_계정을_회원에_연결한다(SocialProvider provider) {
        Member member = new Member("social@example.com", null, "소셜 회원");

        SocialAccount account = new SocialAccount(member, provider, "provider-user-1");

        assertThat(account.getMember()).isSameAs(member);
        assertThat(account.getProvider()).isEqualTo(provider);
        assertThat(account.getProviderUserId()).isEqualTo("provider-user-1");
    }

    @Test
    void 회원과_공급자는_필수다() {
        Member member = new Member("social@example.com", null, "소셜 회원");

        assertThatThrownBy(() -> new SocialAccount(null, SocialProvider.GOOGLE, "user-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("회원은 필수입니다.");
        assertThatThrownBy(() -> new SocialAccount(member, null, "user-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("소셜 공급자는 필수입니다.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 공급자_사용자_식별자는_필수다(String providerUserId) {
        Member member = new Member("social@example.com", null, "소셜 회원");

        assertThatThrownBy(() -> new SocialAccount(member, SocialProvider.GOOGLE, providerUserId))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("소셜 사용자 식별자는 필수입니다.");
    }

    @Test
    void 공급자_사용자_식별자는_255자를_초과할_수_없다() {
        Member member = new Member("social@example.com", null, "소셜 회원");

        assertThatThrownBy(() -> new SocialAccount(member, SocialProvider.GOOGLE, "a".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("255자");
    }
}
