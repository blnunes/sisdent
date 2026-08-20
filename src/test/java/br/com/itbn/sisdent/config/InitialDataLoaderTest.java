package br.com.itbn.sisdent.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitialDataLoaderTest {

    @Mock
    private RandomGenerator random;

    @Test
    void createsUpcomingRandomScheduledTimesAndAHistoricalCompletedRecord() {
        Instant seedTime = Instant.parse("2026-08-17T10:00:00Z");
        when(random.nextLong(1, 301)).thenReturn(217L);

        InitialDataLoader.SeedAppointmentTimes times = InitialDataLoader.randomAppointmentTimes(seedTime, random);

        assertThat(times.scheduledStart()).isEqualTo(Instant.parse("2026-08-17T13:37:00Z"));
        assertThat(times.scheduledStart()).isAfter(seedTime).isBeforeOrEqualTo(seedTime.plus(Duration.ofHours(5)));
        assertThat(times.scheduledEnd()).isEqualTo(times.scheduledStart().plus(Duration.ofMinutes(30)));
        assertThat(times.completedStart()).isEqualTo(times.scheduledStart().minus(Duration.ofDays(1)));
        assertThat(times.completedEnd()).isEqualTo(times.completedStart().plus(Duration.ofMinutes(30)));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 300})
    void keepsScheduledTimesInsideTheOneMinuteToFiveHourWindow(long offsetMinutes) {
        Instant seedTime = Instant.parse("2026-08-17T10:00:00Z");
        when(random.nextLong(1, 301)).thenReturn(offsetMinutes);

        InitialDataLoader.SeedAppointmentTimes times = InitialDataLoader.randomAppointmentTimes(seedTime, random);

        assertThat(times.scheduledStart()).isEqualTo(seedTime.plus(Duration.ofMinutes(offsetMinutes)));
        assertThat(times.scheduledStart()).isAfter(seedTime).isBeforeOrEqualTo(seedTime.plus(Duration.ofHours(5)));
    }
}
