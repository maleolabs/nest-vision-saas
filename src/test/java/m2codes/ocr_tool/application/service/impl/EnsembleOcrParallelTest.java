package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regresi paralelisasi engine: EnsembleOcrService harus:
 * 1. menjalankan engine secara paralel (bukan serial) — dibuktikan dengan engine
 *    lambat (fake 800ms) tidak menahan engine cepat (fake 50ms) keluar lebih dulu.
 * 2. tetap memilih engine dengan kualitas terbaik (isBetter: conf + NIK), bukan
 *    engine yang pertama selesai.
 * 3. early-exit: begitu ada hasil yang isGood, engine yang masih jalan dibatalkan.
 */
class EnsembleOcrParallelTest {

    private OcrResultDto dto(String engine, String text, int conf) {
        OcrResultDto d = new OcrResultDto();
        d.setSuccess(true);
        d.setExtractedText(text);
        d.setConfidence(conf);
        d.setEngineUsed(engine);
        return d;
    }

    private TextExtractionService engineWith(OcrResultDto result, long delayMs) {
        return new TextExtractionService() {
            @Override
            public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
                return result;
            }

            @Override
            public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // dibatalkan early-exit: kembalikan hasil gagal, bukan lempar
                    OcrResultDto cancelled = new OcrResultDto();
                    cancelled.setSuccess(false);
                    cancelled.setEngineUsed(result.getEngineUsed());
                    cancelled.setErrorMessage("cancelled");
                    return cancelled;
                }
                return result;
            }
        };
    }

    @Test
    void parallelPicksHighConfEngineEvenWhenSlower() {
        // rapid: 120ms conf=85 (menang kualitas), native: 10ms conf=46 (cepat tapi lemah)
        EnsembleOcrService svc = new EnsembleOcrService(
                engineWith(dto("pytesseract", "x".repeat(30), 40), 1000),
                engineWith(dto("native-tesseract", "y".repeat(30), 46), 10),
                engineWith(dto("paddle", "z".repeat(30), 30), 1000),
                engineWith(dto("rapid", "NIK 7501070410020002 " + "a".repeat(30), 85), 120));
        ReflectionTestUtils.setField(svc, "parallelEnabled", true);
        ReflectionTestUtils.setField(svc, "confThreshold", 55);

        long t0 = System.currentTimeMillis();
        OcrResultDto best = svc.extractTextFromImage(new File("fake.jpg"), false);
        long elapse = System.currentTimeMillis() - t0;

        assertEquals("rapid", best.getEngineUsed(), "engine conf tertinggi harus menang");
        assertTrue(elapse < 2000, "tidak boleh menunggu engine lambat: elapse=" + elapse + "ms");
    }

    @Test
    void parallelEarlyExitCancelsSlowEngines() {
        AtomicInteger pytesseractCompleted = new AtomicInteger();

        TextExtractionService slowPy = new TextExtractionService() {
            @Override
            public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
                return dto("pytesseract", "x", 30);
            }

            @Override
            public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
                try {
                    Thread.sleep(5000);
                    pytesseractCompleted.incrementAndGet();
                    return dto("pytesseract", "x".repeat(30), 30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    OcrResultDto cancelled = new OcrResultDto();
                    cancelled.setSuccess(false);
                    cancelled.setEngineUsed("pytesseract");
                    return cancelled;
                }
            }
        };

        EnsembleOcrService svc = new EnsembleOcrService(
                slowPy,
                engineWith(dto("native-tesseract", "y".repeat(30), 46), 50),
                engineWith(dto("paddle", "z".repeat(30), 30), 5000),
                engineWith(dto("rapid", "NIK 7501070410020002 " + "a".repeat(30), 85), 150));
        ReflectionTestUtils.setField(svc, "parallelEnabled", true);
        ReflectionTestUtils.setField(svc, "confThreshold", 55);

        long t0 = System.currentTimeMillis();
        OcrResultDto best = svc.extractTextFromImage(new File("fake.jpg"), false);
        long elapse = System.currentTimeMillis() - t0;

        assertEquals("rapid", best.getEngineUsed());
        assertTrue(elapse < 2000, "pytesseract 5s tidak boleh menahan: elapse=" + elapse + "ms");
        assertEquals(0, pytesseractCompleted.get(), "pytesseract harus dibatalkan, bukan dijalankan penuh");
    }

    @Test
    void serialModeAlsoPrefersRapidFirst() {
        // mode serial (low-core) juga harus reorder: rapid pertama, early return saat isGood
        EnsembleOcrService svc = new EnsembleOcrService(
                engineWith(dto("pytesseract", "x".repeat(30), 40), 10),
                engineWith(dto("native-tesseract", "y".repeat(30), 46), 10),
                engineWith(dto("paddle", "z".repeat(30), 30), 10),
                engineWith(dto("rapid", "NIK 7501070410020002 " + "a".repeat(30), 85), 10));
        ReflectionTestUtils.setField(svc, "parallelEnabled", false);
        ReflectionTestUtils.setField(svc, "confThreshold", 55);

        OcrResultDto best = svc.extractTextFromImage(new File("fake.jpg"), false);
        assertEquals("rapid", best.getEngineUsed(), "serial mode: rapid isGood -> early return");
    }

    @Test
    void parallelAllFailFallsBackToSerial() {
        TextExtractionService failing = new TextExtractionService() {
            @Override
            public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
                OcrResultDto f = new OcrResultDto();
                f.setSuccess(false);
                return f;
            }

            @Override
            public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
                OcrResultDto f = new OcrResultDto();
                f.setSuccess(false);
                f.setEngineUsed("fail-engine");
                return f;
            }
        };

        EnsembleOcrService svc = new EnsembleOcrService(failing, failing, failing, failing);
        ReflectionTestUtils.setField(svc, "parallelEnabled", true);
        ReflectionTestUtils.setField(svc, "confThreshold", 55);

        OcrResultDto best = svc.extractTextFromImage(new File("fake.jpg"), false);
        // semua gagal -> best adalah failure synthesized, bukan exception
        assertEquals(false, best.isSuccess());
    }
}
