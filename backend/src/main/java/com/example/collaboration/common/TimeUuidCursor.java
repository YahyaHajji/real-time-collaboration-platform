package com.example.collaboration.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public record TimeUuidCursor(Instant timestamp, UUID id) {

    public static TimeUuidCursor afterStart() {
        return new TimeUuidCursor(Instant.EPOCH, new UUID(0, 0));
    }

    public static TimeUuidCursor beforeNow() {
        return new TimeUuidCursor(
                Instant.now().plusSeconds(1),
                new UUID(Long.MAX_VALUE, Long.MAX_VALUE)
        );
    }

    public static TimeUuidCursor parse(String value) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int separator = decoded.lastIndexOf('|');
            if (separator < 1 || separator == decoded.length() - 1) {
                throw new IllegalArgumentException("Cursor is malformed");
            }
            return new TimeUuidCursor(
                    Instant.parse(decoded.substring(0, separator)),
                    UUID.fromString(decoded.substring(separator + 1))
            );
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new IllegalArgumentException("Cursor is malformed");
        }
    }

    public String encode() {
        String raw = timestamp + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
