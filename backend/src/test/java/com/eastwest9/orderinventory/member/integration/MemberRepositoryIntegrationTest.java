package com.eastwest9.orderinventory.member.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.domain.MemberRole;
import com.eastwest9.orderinventory.member.domain.SocialAccount;
import com.eastwest9.orderinventory.member.domain.SocialProvider;
import com.eastwest9.orderinventory.member.repository.MemberRepository;
import com.eastwest9.orderinventory.member.repository.SocialAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"})
@ActiveProfiles("mysql-integration")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MemberRepositoryIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 회원을_저장하고_이메일로_조회한다() {
        String email = UUID.randomUUID() + "@example.com";
        assertThat(memberRepository.existsByEmail(email)).isFalse();
        assertThat(memberRepository.findByEmail(email)).isEmpty();

        Member saved = memberRepository.saveAndFlush(new Member(email, "test-password", "회원"));
        Member found = memberRepository.findByEmail(email).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("회원");
        assertThat(found.getPassword()).isEqualTo("test-password");
        assertThat(found.getRole()).isEqualTo(MemberRole.USER);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(memberRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void 중복_이메일은_DB_UNIQUE_제약으로_거부한다() {
        String email = UUID.randomUUID() + "@example.com";
        Member original = memberRepository.saveAndFlush(new Member(email, "test-password", "기존 회원"));

        // 사전 exists 조회 없이 두 번째 INSERT를 실제 DB에 실행한다.
        assertThatThrownBy(() -> memberRepository.saveAndFlush(new Member(email, null, "소셜 회원")))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uk_member_email");

        assertThat(memberRepository.findByEmail(email).orElseThrow().getId()).isEqualTo(original.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM social_account WHERE member_id = ?", Integer.class, original.getId())).isZero();
    }

    @ParameterizedTest
    @EnumSource(SocialProvider.class)
    void 공급자_계정을_저장하고_회원_FK로_조회한다(SocialProvider provider) {
        Member member = memberRepository.saveAndFlush(new Member(UUID.randomUUID() + "@example.com", null, "소셜 회원"));
        String providerUserId = UUID.randomUUID().toString();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)).isEmpty();

        SocialAccount saved = socialAccountRepository.saveAndFlush(new SocialAccount(member, provider, providerUserId));
        SocialAccount found = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getMember().getId()).isEqualTo(member.getId());
        assertThat(found.getProvider()).isEqualTo(provider);
        assertThat(found.getProviderUserId()).isEqualTo(providerUserId);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getPassword()).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT member_id FROM social_account WHERE social_account_id = ?", Long.class, saved.getId())).isEqualTo(member.getId());
        assertThat(jdbcTemplate.queryForObject("SELECT provider FROM social_account WHERE social_account_id = ?", String.class, saved.getId())).isEqualTo(provider.name());
    }

    @Test
    void 동일_공급자의_동일_사용자를_다른_회원에_중복_연결할_수_없다() {
        Member first = memberRepository.saveAndFlush(new Member(UUID.randomUUID() + "@example.com", null, "첫 회원"));
        Member second = memberRepository.saveAndFlush(new Member(UUID.randomUUID() + "@example.com", null, "둘째 회원"));
        String providerUserId = UUID.randomUUID().toString();
        SocialAccount original = socialAccountRepository.saveAndFlush(new SocialAccount(first, SocialProvider.GOOGLE, providerUserId));

        assertThatThrownBy(() -> socialAccountRepository.saveAndFlush(new SocialAccount(second, SocialProvider.GOOGLE, providerUserId)))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("uk_social_account_provider_provider_user_id");

        SocialAccount found = socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, providerUserId).orElseThrow();
        assertThat(found.getId()).isEqualTo(original.getId());
        assertThat(found.getMember().getId()).isEqualTo(first.getId());
    }

    @Test
    void 같은_사용자_식별자라도_공급자가_다르면_한_회원에_저장할_수_있다() {
        Member member = memberRepository.saveAndFlush(new Member(UUID.randomUUID() + "@example.com", "test-password", "회원"));
        String providerUserId = UUID.randomUUID().toString();

        SocialAccount google = socialAccountRepository.saveAndFlush(new SocialAccount(member, SocialProvider.GOOGLE, providerUserId));
        SocialAccount kakao = socialAccountRepository.saveAndFlush(new SocialAccount(member, SocialProvider.KAKAO, providerUserId));

        assertThat(google.getId()).isNotEqualTo(kakao.getId());
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, providerUserId).orElseThrow().getMember().getId()).isEqualTo(member.getId());
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId).orElseThrow().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    void 공급자_사용자_식별자의_대소문자를_구분한다() {
        Member member = memberRepository.saveAndFlush(new Member(UUID.randomUUID() + "@example.com", null, "회원"));
        String prefix = UUID.randomUUID().toString();

        SocialAccount upper = socialAccountRepository.saveAndFlush(new SocialAccount(member, SocialProvider.GOOGLE, prefix + "A"));
        SocialAccount lower = socialAccountRepository.saveAndFlush(new SocialAccount(member, SocialProvider.GOOGLE, prefix + "a"));

        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, prefix + "A").orElseThrow().getId()).isEqualTo(upper.getId());
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, prefix + "a").orElseThrow().getId()).isEqualTo(lower.getId());
    }

    @Test
    void 존재하지_않는_회원_참조는_DB_FK_제약으로_거부한다() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO social_account (member_id, provider, provider_user_id) VALUES (?, ?, ?)", Long.MAX_VALUE, "GOOGLE", UUID.randomUUID().toString()))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("fk_social_account_member");
    }

    @Test
    void DB에서도_이메일은_필수이며_기본_권한은_USER다() {
        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO member (member_name, email) VALUES (?, ?)", "회원", null))
                .isInstanceOf(DataIntegrityViolationException.class);

        String email = UUID.randomUUID() + "@example.com";
        jdbcTemplate.update("INSERT INTO member (member_name, email) VALUES (?, ?)", "회원", email);

        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getRole()).isEqualTo(MemberRole.USER);
        assertThat(member.getPassword()).isNull();
    }

    @Test
    void ADMIN_권한을_문자열로_저장하고_조회한다() {
        String email = UUID.randomUUID() + "@example.com";
        jdbcTemplate.update("INSERT INTO member (member_name, email, member_role) VALUES (?, ?, ?)", "관리자", email, "ADMIN");

        assertThat(memberRepository.findByEmail(email).orElseThrow().getRole()).isEqualTo(MemberRole.ADMIN);
    }
}
