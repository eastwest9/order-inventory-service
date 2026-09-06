ALTER TABLE member
    ADD COLUMN email VARCHAR(254) NULL,
    ADD COLUMN password VARCHAR(255) NULL,
    ADD COLUMN member_role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Phase 1 회원에는 이메일이 없으므로 실제 연락처가 아닌 고유 보완 값을 사용한다.
-- 기존 ID, 주문 참조, audit 시각은 보존한다. 인증 도입 시 실제 이메일 확인이 필요하다.
UPDATE member
SET email = CONCAT('legacy-', member_id, '@members.invalid'),
    updated_at = updated_at
WHERE email IS NULL;

ALTER TABLE member
    MODIFY COLUMN email VARCHAR(254) NOT NULL,
    ADD CONSTRAINT uk_member_email UNIQUE (email),
    ADD CONSTRAINT chk_member_role CHECK (member_role IN ('USER', 'ADMIN'));

CREATE TABLE social_account (
    social_account_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) COLLATE utf8mb4_0900_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    created_id CHAR(36) NULL,
    updated_id CHAR(36) NULL,

    CONSTRAINT pk_social_account
        PRIMARY KEY (social_account_id),

    CONSTRAINT uk_social_account_provider_provider_user_id
        UNIQUE (provider, provider_user_id),

    CONSTRAINT fk_social_account_member
        FOREIGN KEY (member_id)
        REFERENCES member (member_id),

    CONSTRAINT chk_social_account_provider
        CHECK (provider IN ('GOOGLE', 'KAKAO'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
