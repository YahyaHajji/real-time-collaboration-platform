package com.example.collaboration.common;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class UuidV7 {

    private UuidV7() {
    }

    public static UUID generate() {
        long timestamp = Clock.systemUTC().millis() & 0x0000_FFFF_FFFF_FFFFL;
        long randomA = ThreadLocalRandom.current().nextLong() & 0xFFFL;
        long randomB = ThreadLocalRandom.current().nextLong() & 0x3FFF_FFFF_FFFF_FFFFL;
        long mostSignificant = (timestamp << 16) | 0x7000L | randomA;
        long leastSignificant = 0x8000_0000_0000_0000L | randomB;
        return new UUID(mostSignificant, leastSignificant);
    }
}

