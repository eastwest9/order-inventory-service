package com.eastwest9.orderinventory.member.domain;

import com.eastwest9.orderinventory.common.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "member_name", nullable = false, length = 100)
    private String name;

    public Member(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("회원명은 필수입니다.");
        }

        if (name.length() > 100) {
            throw new IllegalArgumentException("회원명은 100자를 초과할 수 없습니다.");
        }

        this.name = name;
    }
}
