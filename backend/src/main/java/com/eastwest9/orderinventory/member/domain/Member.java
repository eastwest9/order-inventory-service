package com.eastwest9.orderinventory.member.domain;

import com.eastwest9.orderinventory.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "member_uuid", unique = true, length = 36)
    private String memberUuid;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "member_name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 20)
    private MemberRole role;

    public Member(String email, String password, String name) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }

        if (email.length() > 254) {
            throw new IllegalArgumentException("이메일은 254자를 초과할 수 없습니다.");
        }

        if (password != null && (password.isBlank() || password.length() > 255)) {
            throw new IllegalArgumentException("비밀번호는 값이 있으면 공백이 아니고 255자 이하여야 합니다.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("회원명은 필수입니다.");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("회원명은 100자를 초과할 수 없습니다.");
        }

        this.email = email;
        this.password = password;
        this.name = name;
        this.role = MemberRole.USER;
    }
}
