package m2codes.ocr_tool.domain.model;

import jakarta.persistence.*;
import lombok.*;
import m2codes.ocr_tool.infrastructure.security.util.QuotaDayKey;

import java.time.LocalDate;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
// VIS-F13: unique (client_id, request_date) sebagai dukungan atomic upsert
// (INSERT ... ON DUPLICATE KEY UPDATE) — tanpa ini ODKU selalu insert baris baru.
// ddl-auto=update akan menambahkan constraint; bila data lama punya duplikat,
// bersihkan duplikat dulu (lihat ops notes di report).
@Table(name = "request_usages", uniqueConstraints = @UniqueConstraint(
        name = "uk_request_usages_client_date",
        columnNames = {"client_id", "request_date"}))
public class RequestUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    // kolom tetap DATE (tidak ada DDL). Instant → LocalDate (D9/VIS-F14): type-safe
    // day-key WIB via QuotaDayKey — binding LocalDate ke DATE tidak ambigu zona.
    @Column(name = "request_date", nullable = false, updatable = false, columnDefinition = "DATE")
    private LocalDate requestDate;

    @PrePersist
    protected void onCreate() {
        // pengaman jika service lupa mengeset tanggal
        requestDate = QuotaDayKey.today();
    }

}
