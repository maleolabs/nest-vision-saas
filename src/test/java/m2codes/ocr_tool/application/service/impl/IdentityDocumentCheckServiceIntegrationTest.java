package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.interfaces.dto.response.IdentityDocumentCheckResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * Integration test alur identity: pass tunggal OCR verifikasi → persist
 * ocr_result + extracted_text → GET mengembalikannya.
 *
 * Pakai MySQL test profile (ocr_tool_test) sehingga FK insert, transaksi
 * @Async worker, dan lazy OneToOne di get() benar-benar tereksekusi tanpa mock
 * pada layer repository. Hanya mesin OCR yang di-stub (python/tesseract
 * tidak tersedia di environment test).
 */
@SpringBootTest
@ActiveProfiles("test")
class IdentityDocumentCheckServiceIntegrationTest {

    @Autowired
    private IdentityDocumentCheckServiceImpl service;

    @Autowired
    private IdentityDocumentCheckWorker worker;

    @Autowired
    private OcrRequestRepository requests;

    @MockBean
    @Qualifier("ensembleOcrService")
    private TextExtractionService ensembleOcr;

    private static final String KTP_TEXT = String.join("\n",
            "PROVINSI GORONTALO",
            "KABUPATEN GORONTALO",
            "NIK: 1234567890123456",
            "NAMA: BUDI SANTOSO",
            "TEMPAT/TGL LAHIR: GORONTALO, 01-01-1990",
            "JENIS KELAMIN: LAKI-LAKI",
            "ALAMAT: JL MERDEKA",
            "RT/RW: 001/002",
            "KELURAHAN: BULOTEA",
            "KECAMATAN: BONE",
            "AGAMA: ISLAM",
            "KTP"
    );

    @Test
    void acceptedKtpPersistsFieldsAndStatusRoundTrip() {
        when(ensembleOcr.extractTextFromImage(any(File.class), anyBoolean()))
                .thenReturn(OcrResultDto.builder()
                        .isSuccess(true)
                        .extractedText(KTP_TEXT)
                        .duration(120L)
                        .build());

        OcrRequest request = requests.save(OcrRequest.builder()
                .imageUrl("integration-" + UUID.randomUUID())
                .clientId("integration-client")
                .status(RequestStatus.PROCESSING)
                .build());
        UUID id = request.getId();

        worker.process(id, "integration-client", new File("nonexistent.jpg"),
                List.of("nik", "nama", "provinsi", "kabupaten", "kecamatan", "kelurahan"));

        // Worker @Async: tunggu sampai status & field tervisualisasi di DB.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            OcrRequest row = requests.findById(id).orElseThrow();
            assertThat(row.getStatus()).isEqualTo(RequestStatus.DONE);
            assertThat(row.getIdentityDecision()).isEqualTo("ACCEPTED");
            assertThat(row.getIdentityDocumentType()).isEqualTo("KTP");
        });

        IdentityDocumentCheckResponse response = service.get(id.toString(), "integration-client");
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getDecision()).isEqualTo("ACCEPTED");
        assertThat(response.getExtractedFields())
                .extracting("textKey")
                .contains("nik", "nama");
        assertThat(response.getExtractedFields())
                .extracting("textValue")
                .contains("1234567890123456", "BUDI SANTOSO");

        // Cleanup baris test (FK cascade via OneToOne ALL).
        requests.deleteById(id);
    }
}
