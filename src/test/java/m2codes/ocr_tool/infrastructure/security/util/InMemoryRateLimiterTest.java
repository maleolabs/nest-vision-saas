package m2codes.ocr_tool.infrastructure.security.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test murni sliding-window InMemoryRateLimiter (VIS-F05), tanpa Spring context.
 *
 * UNEXECUTED di sandbox (tanpa javac). Jalankan `mvn test` di dev env.
 */
class InMemoryRateLimiterTest {

    private final InMemoryRateLimiter limiter = new InMemoryRateLimiter();

    @Test
    void allowsUpToMaxHitsThenRejects() {
        String key = "acct:test-user";
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow(key, 5, 60_000L), "hit ke-" + (i + 1) + " harus diizinkan");
        }
        assertFalse(limiter.allow(key, 5, 60_000L), "hit ke-6 harus ditolak");
    }

    @Test
    void differentKeysAreIndependent() {
        assertTrue(limiter.allow("acct:user-a", 1, 60_000L));
        assertFalse(limiter.allow("acct:user-a", 1, 60_000L));
        assertTrue(limiter.allow("acct:user-b", 1, 60_000L));
    }

    @Test
    void resetClearsCountForSuccessCase() {
        String key = "acct:reset-user";
        assertTrue(limiter.allow(key, 2, 60_000L));
        assertTrue(limiter.allow(key, 2, 60_000L));
        assertFalse(limiter.allow(key, 2, 60_000L));

        // reset setelah verifikasi sukses → hitungan bersih
        limiter.reset(key);
        assertTrue(limiter.allow(key, 2, 60_000L));
    }

    @Test
    void rejectedHitsDoNotExtendWindow() {
        String key = "acct:reject-user";
        assertTrue(limiter.allow(key, 1, 50L));
        // flood ditolak tanpa mencatat — window 50ms lewat, hit pertama expired
        try {
            Thread.sleep(60L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(limiter.allow(key, 1, 50L), "window baru harus diizinkan lagi");
    }

    @Test
    void slidingWindowExcludesExpiredHits() {
        String key = "acct:slide-user";
        assertTrue(limiter.allow(key, 2, 60L));
        try {
            Thread.sleep(80L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(limiter.allow(key, 2, 60L));
        assertTrue(limiter.allow(key, 2, 60L), "hit kadaluarsa tidak dihitung dalam window baru");
        assertFalse(limiter.allow(key, 2, 60L));
    }
}
