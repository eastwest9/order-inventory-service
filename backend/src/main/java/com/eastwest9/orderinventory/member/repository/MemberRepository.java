package com.eastwest9.orderinventory.member.repository;

import com.eastwest9.orderinventory.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
