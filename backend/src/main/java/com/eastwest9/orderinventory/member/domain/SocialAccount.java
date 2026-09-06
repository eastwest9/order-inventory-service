package com.eastwest9.orderinventory.member.domain;

import com.eastwest9.orderinventory.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "social_account", uniqueConstraints = @UniqueConstraint(name = "uk_social_account_provider_provider_user_id", columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    public SocialAccount(Member member, SocialProvider provider, String providerUserId) {
        if (member == null) {
            throw new IllegalArgumentException("회원은 필수입니다.");
        }

        if (provider == null) {
            throw new IllegalArgumentException("소셜 공급자는 필수입니다.");
        }

        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 사용자 식별자는 필수입니다.");
        }

        if (providerUserId.length() > 255) {
            throw new IllegalArgumentException("소셜 사용자 식별자는 255자를 초과할 수 없습니다.");
        }

        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}
