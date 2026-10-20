package m2codes.ocr_tool.infrastructure.security.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.RequestUsage;
import m2codes.ocr_tool.domain.repository.RequestUsageRepository;
import m2codes.ocr_tool.domain.service.ClientService;
import m2codes.ocr_tool.infrastructure.security.util.QuotaDayKey;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLimitService {

    private final RequestUsageRepository requestUsageRepository;
    private final ClientService clientService;

    /**
     * Pre-check baca (sebelum OCR dijalankan). Kuota dipakai tetap hanya pada request
     * yang sukses (incrementUsage dipanggil di path success oleh OcrController), jadi
     * check di sini tetap soft-read — cap KERAS dipaksa di incrementUsage (atomic).
     */
    public boolean isRequestAllowed(String clientId) {
        Client client = clientService.findByClientId(clientId).orElse(null);
        if (client == null) {
            return false;
        }

        int maxRequest = client.getAccountType().getDailyLimit();

        int used = requestUsageRepository.findByClientIdAndRequestDate(clientId, QuotaDayKey.today())
                .map(RequestUsage::getRequestCount)
                .orElse(0);

        return used < maxRequest;
    }

    /**
     * VIS-F13: increment ATOMIK via SQL bersyarat. Versi lama (findBy → +1 → save)
     * bisa over-quota saat request success concurrent.
     * Alur: conditional UPDATE (affected > 0 = allowed). Jika 0 → row hari ini mungkin
     * belum ada → INSERT IGNORE lalu conditional UPDATE diulang dalam transaksi yang
     * sama; jika tetap 0 berarti sudah mencapai cap → hitungan TIDAK bertambah.
     * (Bukan INSERT ... ON DUPLICATE KEY UPDATE: Connector/J default melaporkan
     * "found rows" sehingga no-op ODKU tak bisa dibedakan dari sukses — lihat
     * javadoc RequestUsageRepository.) Dengan unique constraint
     * (client_id, request_date), total per hari tidak bisa melebihi dailyLimit walau race.
     *
     * D9/VIS-F14: day key WIB via QuotaDayKey (satu definisi "hari" bersama dashboard).
     */
    @Transactional
    public void incrementUsage(String clientId) {
        int maxRequest = clientService.findByClientId(clientId)
                .map(client -> client.getAccountType().getDailyLimit())
                .orElse(0);
        if (maxRequest <= 0) {
            log.warn("No daily limit configured for client {}, skipping usage increment", clientId);
            return;
        }

        LocalDate day = QuotaDayKey.today();
        int affected = requestUsageRepository.incrementRequestCountIfBelowLimit(clientId, day, maxRequest);
        if (affected == 0) {
            // row hari ini belum ada → buat (INSERT IGNORE, no-op bila duplikat),
            // lalu conditional UPDATE diulang: tetap 0 berarti sudah di cap → denied
            requestUsageRepository.createRowIfMissing(clientId, day);
            requestUsageRepository.incrementRequestCountIfBelowLimit(clientId, day, maxRequest);
        }
    }

    /**
     * VIS-F13/D9: jumlah pemakaian hari ini (WIB) via satu SELECT — read tanpa race
     * untuk dashboard tampilan kuota (belum ada konsumen; siap dipakai).
     */
    public int getUsedCountToday(String clientId) {
        return requestUsageRepository.findByClientIdAndRequestDate(clientId, QuotaDayKey.today())
                .map(RequestUsage::getRequestCount)
                .orElse(0);
    }

}
