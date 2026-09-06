package com.eastwest9.orderinventory.member.dto;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.domain.MemberRole;

public record MemberResponseDto(
        Long memberId,
        String email,
        String name,
        MemberRole role
) {

    public static MemberResponseDto from(Member member) {
        return new MemberResponseDto(member.getId(), member.getEmail(), member.getName(), member.getRole());
    }
}
