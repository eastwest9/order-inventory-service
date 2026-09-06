package com.eastwest9.orderinventory.member.exception;

public class DuplicateMemberEmailException extends RuntimeException {

    public DuplicateMemberEmailException(String email) {
        super("이미 사용 중인 이메일입니다. email=" + email);
    }
}
