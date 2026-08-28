package com.example.collaboration.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogIdentifierTest {

    @Test
    void producesStableTruncatedHashesWithoutExposingTheInput() {
        String hash = LogIdentifier.hash("session-123");

        assertThat(hash).hasSize(16).doesNotContain("session-123");
        assertThat(LogIdentifier.hash("session-123")).isEqualTo(hash);
        assertThat(LogIdentifier.hash(null)).isEqualTo("none");
    }
}
