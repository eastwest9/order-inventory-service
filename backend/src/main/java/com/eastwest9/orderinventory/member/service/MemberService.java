package com.eastwest9.orderinventory.member.service;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.dto.MemberCreateRequestDto;
import com.eastwest9.orderinventory.member.dto.MemberResponseDto;
import com.eastwest9.orderinventory.member.exception.DuplicateMemberEmailException;
import com.eastwest9.orderinventory.member.repository.MemberRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final String MEMBER_EMAIL_UNIQUE_CONSTRAINT = "uk_member_email";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponseDto createMember(MemberCreateRequestDto request) {
        String email = normalizeEmail(request.email());
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateMemberEmailException(email);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = new Member(email, encodedPassword, request.name());

        try {
            return MemberResponseDto.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, MEMBER_EMAIL_UNIQUE_CONSTRAINT)) {
                throw new DuplicateMemberEmailException(email);
            }
            throw exception;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
