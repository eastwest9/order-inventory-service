package com.eastwest9.orderinventory.member.exception;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long memberId) {
        super("회원을 찾을 수 없습니다. memberId=" + memberId);
    }
}
