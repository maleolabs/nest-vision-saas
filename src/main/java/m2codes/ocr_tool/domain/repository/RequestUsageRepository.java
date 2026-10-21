package m2codes.ocr_tool.domain.repository;

import m2codes.ocr_tool.domain.model.RequestUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * VIS-F13: native mutating untuk kuota daily. Semua native di sini; pemanggil
 * wajib membawa transaksi aktif (@Transactional pada service).
 *
 * PENTING (kenapa bukan INSERT ... ON DUPLICATE KEY UPDATE): Connector/J default
 * melaporkan "found rows" (useAffectedRows=false) — ODKU no-op saat di-cap akan
 * dilaporkan affected=1 sehingga "0 = denied" tidak bisa dibaca. Pola yang dipakai:
 * (1) UPDATE bersyarat (count < cap) — matched == changed, aman di kedua mode;
 * (2) INSERT IGNORE (satu baris, selalu 0 atau 1 tanpa ambiguitas);
 * lalu UPDATE bersyarat diulang dalam transaksi yang sama.
 */
public interface RequestUsageRepository extends JpaRepository<RequestUsage, Long> {

    Optional<RequestUsage> findByClientIdAndRequestDate(String clientId, LocalDate requestDate);

    /**
     * Increment bersyarat ATOMIK di SQL — affected rows >= 1 berarti hitungan masih
     * di bawah cap (allowed); 0 = sudah mencapai cap atau row hari ini belum ada.
     * Menutup check-then-act race di service.
     */
    @Modifying
    @Query(value = """
            UPDATE request_usages
            SET request_count = request_count + 1
            WHERE client_id = :clientId
              AND request_date = :day
              AND request_count < :maxCount
            """, nativeQuery = true)
    int incrementRequestCountIfBelowLimit(@Param("clientId") String clientId, @Param("day") LocalDate day, @Param("maxCount") int maxCount);

    /**
     * Buat row counter untuk (client_id, request_date) bila belum ada;
     * row yang sudah ada tidak disentuh (INSERT IGNORE → affected 0). Unique
     * constraint (client_id, request_date) di entity adalah prasyarat jalur ini.
     */
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO request_usages (client_id, request_count, request_date)
            VALUES (:clientId, 0, :day)
            """, nativeQuery = true)
    int createRowIfMissing(@Param("clientId") String clientId, @Param("day") LocalDate day);

}
