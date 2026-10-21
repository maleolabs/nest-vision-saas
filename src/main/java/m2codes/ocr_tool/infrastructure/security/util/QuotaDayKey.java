package m2codes.ocr_tool.infrastructure.security.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * D9 / VIS-F14: definisi "hari" kuota TETAP di satu tempat — WIB (Asia/Jakarta).
 *
 * Sebelumnya RequestLimitService memakai UTC (LocalDate.now(ZoneOffset.UTC)) sementara
 * query dashboard (ApiRequestLogRepository) memakai CONVERT_TZ('+00:00','+07:00') di SQL,
 * sehingga boundary "hari" berbeda 7 jam — kuota vs dashboard bisa tidak sinkron dekat
 * tengah malam WIB. Indonesia tanpa DST → +07:00 konstanta, setara Asia/Jakarta.
 *
 * Perubahan hari ini Java-level saja (kolom tetap DATE) — tidak perlu DDL/migration.
 */
public final class QuotaDayKey {

    /** zona operasional; dipakai juga oleh query SQL dashboard via CONVERT_TZ '+07:00' */
    public static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");

    private QuotaDayKey() {
    }

    /** day-key berjalan (WIB) untuk batas kuota daily */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** day-key dari timestamp (WIB) — untuk analytics/backfill tanpa konversi manual */
    public static LocalDate fromInstant(Instant instant) {
        return LocalDate.ofInstant(instant, ZONE);
    }
}
