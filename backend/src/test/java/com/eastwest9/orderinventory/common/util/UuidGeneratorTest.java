package com.eastwest9.orderinventory.common.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class UuidGeneratorTest {

    @Test
    void 표준_UUID_문자열을_생성한다() {
        String generated = UuidGenerator.generate();

        assertThat(generated).hasSize(36);
        assertThatCode(() -> UUID.fromString(generated))
                .doesNotThrowAnyException();
    }
}
