package m2codes.ocr_tool.infrastructure.security.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test murni day-key kuota (D9/VIS-F14): Instant → LocalDate di WIB (Asia/Jakarta).
 *
 * UNEXECUTED di sandbox (tanpa javac). Jalankan `mvn test` di dev env.
 */
class QuotaDayKeyTest {

    @Test
    void utcMidnightMapsToSameWibDay() {
        // 2026-01-01T00:00:00Z = 2026-01-01T07:00 WIB → hari yang sama
        assertEquals(LocalDate.of(2026, 1, 1),
                QuotaDayKey.fromInstant(Instant.parse("2026-01-01T00:00:00Z")));
    }

    @Test
    void lateUtcAfternoonRoundsUpToNextWibDay() {
        // 2026-01-01T17:00:00Z = 2026-01-02T00:00 WIB → hari berikutnya
        assertEquals(LocalDate.of(2026, 1, 2),
                QuotaDayKey.fromInstant(Instant.parse("2026-01-01T17:00:00Z")));
    }

    @Test
    void epochStartsAt07amWib() {
        assertEquals(LocalDate.of(1970, 1, 1), QuotaDayKey.fromInstant(Instant.EPOCH));
    }

    @Test
    void instantSixteenFiftyNineUtcStaysPrevWibDay() {
        // 2026-01-01T16:59:00Z = 2026-01-01T23:59 WIB → masih hari yang sama
        assertEquals(LocalDate.of(2026, 1, 1),
                QuotaDayKey.fromInstant(Instant.parse("2026-01-01T16:59:00Z")));
    }
}
