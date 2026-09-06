package com.eastwest9.orderinventory.member.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class MemberMigrationIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Test
    void V3_적용시_기존_회원과_주문_참조를_보존한다() {
        // Given: 기존 V2 스키마에 이메일 없는 Phase 1 회원과 주문이 존재한다.
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).target("2").load().migrate();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0));
        Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2025, 2, 1, 0, 0));
        String memberUuid = "00000000-0000-0000-0000-000000000001";
        jdbcTemplate.update("INSERT INTO member (member_id, member_name, member_uuid, created_at, updated_at, created_id, updated_id) VALUES (?, ?, ?, ?, ?, ?, ?)", 1L, "기존 회원", memberUuid, createdAt, updatedAt, memberUuid, memberUuid);
        jdbcTemplate.update("INSERT INTO member (member_id, member_name) VALUES (?, ?)", 2L, "둘째 회원");
        jdbcTemplate.update("INSERT INTO shop_order (order_id, order_number, member_id, order_status, total_amount) VALUES (?, ?, ?, ?, ?)", 1L, "LEGACY-ORDER-1", 1L, "CREATED", 0);

        // When
        Flyway flyway = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();

        // Then: ID, UUID, 이름, audit 값과 주문 FK는 그대로 유지된다.
        assertThat(jdbcTemplate.queryForList("SELECT email FROM member ORDER BY member_id", String.class))
                .containsExactly("legacy-1@members.invalid", "legacy-2@members.invalid");
        assertThat(jdbcTemplate.queryForList("SELECT member_role FROM member ORDER BY member_id", String.class))
                .containsExactly("USER", "USER");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE password IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT member_name FROM member WHERE member_id = 1", String.class)).isEqualTo("기존 회원");
        assertThat(jdbcTemplate.queryForObject("SELECT member_uuid FROM member WHERE member_id = 1", String.class)).isEqualTo(memberUuid);
        assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM member WHERE member_id = 1", Timestamp.class)).isEqualTo(createdAt);
        assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM member WHERE member_id = 1", Timestamp.class)).isEqualTo(updatedAt);
        assertThat(jdbcTemplate.queryForObject("SELECT created_id FROM member WHERE member_id = 1", String.class)).isEqualTo(memberUuid);
        assertThat(jdbcTemplate.queryForObject("SELECT updated_id FROM member WHERE member_id = 1", String.class)).isEqualTo(memberUuid);
        assertThat(jdbcTemplate.queryForObject("SELECT m.member_id FROM shop_order o JOIN member m ON m.member_id = o.member_id WHERE o.order_id = 1", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM social_account", Integer.class)).isZero();
    }
}
