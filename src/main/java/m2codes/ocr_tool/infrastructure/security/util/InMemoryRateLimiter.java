package m2codes.ocr_tool.infrastructure.security.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VIS-F05: sliding-window rate limiter in-memory (tanpa dependency baru).
 *
 * Trade-off (disengaja, tulis eksplisit di sini):
 * - SINGLE INSTANCE: hanya melindungi satu node — deployment multi-instance
 *   butuh store berbagi (Redis/gateway) yang saat ini tidak tersedia di project.
 * - State hilang saat restart (penyerang bisa menunggu restart untuk reset gratis).
 * - Memori dibatasi sweep opportunistic tiap N-th pemanggilan allow() — key yang
 *   idle lebih lama dari SWEEP_IDLE_MILLIS dibuang.
 * Primitive: ConcurrentHashMap dengan per-key lock via compute() → aman concurrent;
 * hit yang ditolak tidak dicatat sehingga flood tidak memperpanjang blokir.
 */
@Component
public class InMemoryRateLimiter {

    private static final int SWEEP_EVERY_N_CALLS = 256;
    /** stale threshold: key terakhirnya lebih tua dari ini dibuang saat sweep */
    private static final long SWEEP_IDLE_MILLIS = 15 * 60_000L;

    private final ConcurrentHashMap<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final AtomicLong callCounter = new AtomicLong();

    /**
     * Sliding window per key: return true DAN mencatat hit bila jumlah hit dalam
     * window terakhir masih di bawah max; false (tanpa mencatat) bila sudah penuh.
     */
    public boolean allow(String key, int maxHits, long windowMillis) {
        maybeSweep();
        Instant now = Instant.now();
        Instant windowStart = now.minusMillis(windowMillis);

        final boolean[] allowed = {false};
        hits.compute(key, (k, existing) -> {
            Deque<Instant> window = (existing == null) ? new ArrayDeque<>() : existing;
            window.removeIf(hit -> hit.isBefore(windowStart));
            if (window.size() >= maxHits) {
                return window;
            }
            window.addLast(now);
            allowed[0] = true;
            return window;
        });
        return allowed[0];
    }

    /**
     * VIS-F05: reset setelah verifikasi SUKSES — hitungan percobaan bersih mulai.
     */
    public void reset(String key) {
        hits.remove(key);
    }

    /**
     * Sweep opportunistik setiap N-th call: buang key yang hit terakhirnya lebih tua
     * dari SWEEP_IDLE_MILLIS (semua window default ≤ 10 menit). values().removeIf
     * melakukan per-bin locking internal — aman concurrent, tanpa global lock.
     */
    private void maybeSweep() {
        if (callCounter.incrementAndGet() % SWEEP_EVERY_N_CALLS != 0) {
            return;
        }
        Instant retention = Instant.now().minusMillis(SWEEP_IDLE_MILLIS);
        hits.values().removeIf(window -> {
            Instant lastHit = window.peekLast();
            return lastHit == null || lastHit.isBefore(retention);
        });
    }
}
