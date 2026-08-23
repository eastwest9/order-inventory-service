package com.eastwest9.orderinventory.common.util;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
