package com.example.collaboration.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void generatedIdsUseVersionSevenAndRemainTimeSortable() throws Exception {
        UUID first = UuidV7.generate();
        Thread.sleep(2);
        UUID second = UuidV7.generate();

        assertThat(first.version()).isEqualTo(7);
        assertThat(first.variant()).isEqualTo(2);
        List<UUID> sorted = new ArrayList<>(List.of(second, first));
        sorted.sort(Comparator.naturalOrder());
        assertThat(sorted).containsExactly(first, second);
    }
}

