package com.eastwest9.orderinventory.member.repository;

import com.eastwest9.orderinventory.member.domain.SocialAccount;
import com.eastwest9.orderinventory.member.domain.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}
