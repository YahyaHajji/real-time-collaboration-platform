package com.example.collaboration.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TimeUuidCursorTest {

    @Test
    void opaqueCursorRoundTripsBothOrderingFields() {
        TimeUuidCursor cursor = new TimeUuidCursor(
                Instant.parse("2026-08-17T00:00:00.123456Z"),
                UUID.fromString("0198b230-1abc-7000-8000-000000000001")
        );
        assertThat(TimeUuidCursor.parse(cursor.encode())).isEqualTo(cursor);
        assertThatThrownBy(() -> TimeUuidCursor.parse("not-a-cursor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cursor is malformed");
    }
}
