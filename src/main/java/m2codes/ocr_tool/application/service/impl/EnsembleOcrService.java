package m2codes.ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * P2.6 Ensemble OCR: eksekusi paralel semua engine (pytesseract, native
 * tesseract, rapid, paddle) dengan early-exit — engine pertama yang selesai
 * menang, engine lain dibatalkan.
 *
 * <p><b>Arsitektur paralel:</b> unit eksekusi sebenarnya adalah proses eksternal
 * (python subprocess) atau panggilan JNI, bukan thread Java. Virtual thread
 * Java hanya membungkus blocking I/O subprocess sehingga murah dan aman
 * dimultiplex. {@link ExecutorService#invokeAny} dipilih karena semantiknya
 * tepat untuk race ini:
 * <ul>
 *   <li>kembali begitu salah satu task selesai, dan membatalkan task lain —
 *   pytesseract 54s tidak menahan hasil rapid yang sudah selesai 12.7s</li>
 *   <li>API stabil sejak Java 21 (StructuredTaskScope masih preview di Java 25,
 *   butuh --enable-preview — tidak layak untuk production)</li>
 *   <li>virtual thread cocok: task murni blocking-I/O subprocess</li>
 * </ul>
 *
 * <p><b>Catatan mesin low-core:</b> paralel 4 engine butuh ~3-4 core. Pada
 * mesin 1-2 core, paralel malah melambat karena context-switch — karena itu
 * bisa dimatikan lewat {@code ocr.ensemble.parallel=false} untuk kembali ke
 * mode serial yang juga sudah di-reorder (rapid paling depan).
 *
 * <p>Juga menerapkan koreksi whitelist NIK (logging observability).
 */
@Slf4j
@Service(value = "ensembleOcrService")
public class EnsembleOcrService implements TextExtractionService {

    private final TextExtractionService primary; // pyTesseract
    private final TextExtractionService nativeTesseract;
    private final TextExtractionService paddle;
    private final TextExtractionService rapid;

    @Value("${ocr.ensemble.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${ocr.ensemble.conf-threshold:55}")
    private int confThreshold;

    /**
     * Paralel engine aktif secara default. Pada mesin dengan core sangat terbatas,
     * set {@code false} untuk kembali ke mode serial (lihat comment kelas).
     */
    @Value("${ocr.ensemble.parallel:true}")
    private boolean parallelEnabled;

    public EnsembleOcrService(
            @Qualifier("pyTesseractService") TextExtractionService primary,
            @Qualifier("nativeTesseractService") TextExtractionService nativeTesseract,
            @Qualifier("paddleOcrService") TextExtractionService paddle,
            @Qualifier("rapidOcrService") TextExtractionService rapid) {
        this.primary = primary;
        this.nativeTesseract = nativeTesseract;
        this.paddle = paddle;
        this.rapid = rapid;
    }

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto r1 = guardedExtract(primary, "pytesseract", imageUrl, preprocessed);
        log.info("[Ensemble URL] primary success={} conf={} len={}", r1.isSuccess(), r1.getConfidence(), r1.getExtractedText() != null ? r1.getExtractedText().length() : 0);
        if (isGood(r1)) return withEngine(r1, "pytesseract");

        if (!fallbackEnabled) return r1;

        // try native with preprocess forced
        log.info("[Ensemble URL] trying native fallback");
        OcrResultDto r2 = guardedExtract(nativeTesseract, "native-tesseract", imageUrl, true);
        if (isBetter(r2, r1)) {
            log.info("[Ensemble URL] native wins conf={}", r2.getConfidence());
            return withEngine(r2, "native-tesseract");
        }
        return withEngine(r1, "pytesseract");
    }

    /**
     * Ekstraksi dari file: paralel semua engine dengan early-exit (lihat comment kelas).
     */
    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        if (!parallelEnabled) {
            return extractSerial(file, preprocessed);
        }

        List<Callable<OcrResultDto>> tasks = List.of(
                () -> guardedExtract(rapid, "rapid", file, preprocessed),
                () -> guardedExtract(nativeTesseract, "native-tesseract", file, true),
                () -> guardedExtract(primary, "pytesseract", file, preprocessed),
                () -> guardedExtract(paddle, "paddle", file, preprocessed));

        // ponytail: CompletionService + poll 250ms = early-exit berbasis kualitas.
        // invokeAny tidak bisa dipakai: ia ambil task pertama yang selesai tanpa
        // lihat kualitas — native 3.9s conf=46 akan menang duluan dan menenggelamkan
        // rapid 12.7s conf=85. Poll cek isGood() setiap selesai; bila belum ada
        // yang bagus, tunggu sampai semua selesai lalu pilih isBetter().
        // Upgrade path: Java 26+ StructuredTaskScope.ShutdownOnSuccess saat final.
        long t0 = System.currentTimeMillis();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.CompletionService<OcrResultDto> cs =
                    new java.util.concurrent.ExecutorCompletionService<>(pool);
            List<java.util.concurrent.Future<OcrResultDto>> futures = new java.util.ArrayList<>();
            for (Callable<OcrResultDto> t : tasks) futures.add(cs.submit(t));

            OcrResultDto best = null;
            int pending = futures.size();
            while (pending > 0) {
                java.util.concurrent.Future<OcrResultDto> done = cs.poll(250, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (done == null) continue;
                pending--;
                OcrResultDto r;
                try {
                    r = done.get();
                } catch (Exception ex) {
                    continue;
                }
                if (r == null) continue;
                if (best == null || isBetter(r, best)) best = r;
                if (isGood(best)) break; // early-exit: sudah ada hasil bagus, batalkan sisanya
            }
            // Batalkan engine yang masih jalan (subprocess memantau interrupt lalu destroyForcibly).
            for (java.util.concurrent.Future<OcrResultDto> f : futures) f.cancel(true);

            if (best == null) {
                log.warn("[Ensemble Parallel] semua engine gagal, fallback ke serial");
                return extractSerial(file, preprocessed);
            }
            log.info("[Ensemble Parallel] best engine={} conf={} len={} elapse={}ms",
                    best.getEngineUsed(), best.getConfidence(),
                    best.getExtractedText() != null ? best.getExtractedText().length() : 0,
                    System.currentTimeMillis() - t0);
            return ensureEngine(best);
        } catch (Exception e) {
            log.warn("[Ensemble Parallel] race gagal ({}), fallback ke serial", e.getMessage());
            return extractSerial(file, preprocessed);
        }
    }

    /**
     * Mode serial (low-core fallback): urut direorder — rapid paling depan karena
     * benchmark menunjukkan rapid paling cepat + confidence tertinggi untuk KTP,
     * sehingga engine berat (pytesseract) sering tidak perlu dijalankan sama sekali.
     */
    private OcrResultDto extractSerial(File file, boolean preprocessed) {
        OcrResultDto rRapid = guardedExtract(rapid, "rapid", file, preprocessed);
        log.info("[Ensemble Serial] rapid success={} conf={}", rRapid.isSuccess(), rRapid.getConfidence());
        if (isGood(rRapid)) return withEngine(rRapid, "rapid");

        OcrResultDto r2 = guardedExtract(nativeTesseract, "native-tesseract", file, true);
        log.info("[Ensemble Serial] native conf={}", r2.getConfidence());

        OcrResultDto r1 = guardedExtract(primary, "pytesseract", file, preprocessed);
        log.info("[Ensemble Serial] pytesseract success={} conf={}", r1.isSuccess(), r1.getConfidence());

        OcrResultDto r3 = guardedExtract(paddle, "paddle", file, preprocessed);
        log.info("[Ensemble Serial] paddle success={} len={}", r3.isSuccess(), r3.getExtractedText() != null ? r3.getExtractedText().length() : 0);

        OcrResultDto best = r1;
        if (rRapid.isSuccess() && isBetter(rRapid, best)) best = withEngine(rRapid, "rapid");
        if (rRapid.isSuccess() && hasValidNik(rRapid) && !hasValidNik(best)) best = withEngine(rRapid, "rapid");
        if (isBetter(r2, best)) best = withEngine(r2, "native-tesseract");
        if (r3.isSuccess() && isBetter(r3, best)) best = withEngine(r3, "paddle");

        log.info("[Ensemble] chosen engine={} conf={} dur={} len={} nikValid={}", best.getEngineUsed(), best.getConfidence(), best.getDuration(), best.getExtractedText() != null ? best.getExtractedText().length() : 0, hasValidNik(best));
        return best;
    }

    private OcrResultDto ensureEngine(OcrResultDto r) {
        if (r.getEngineUsed() == null || r.getEngineUsed().isBlank()) {
            r.setEngineUsed("ensemble-parallel");
        }
        return r;
    }

    /**
     * Resilience boundary: a single failing engine must never kill the async OCR
     * pipeline. Catches Throwable deliberately - engine failures include Errors such
     * as UnsatisfiedLinkError (native library problems), which would otherwise escape
     * the @Async void method into SimpleAsyncUncaughtExceptionHandler and leave the
     * OcrRequest stuck in a non-terminal status forever. A failed engine is
     * synthesized as an unsuccessful result so the existing isGood/isBetter selection
     * logic simply discards it.
     */
    private OcrResultDto guardedExtract(TextExtractionService engine, String engineName, String imageUrl, boolean preprocessed) {
        try {
            return engine.extractTextFromImage(imageUrl, preprocessed);
        } catch (Throwable t) {
            return synthesizedFailure(engineName, t);
        }
    }

    private OcrResultDto guardedExtract(TextExtractionService engine, String engineName, File file, boolean preprocessed) {
        try {
            return engine.extractTextFromImage(file, preprocessed);
        } catch (Throwable t) {
            return synthesizedFailure(engineName, t);
        }
    }

    private OcrResultDto synthesizedFailure(String engineName, Throwable t) {
        log.error("[Ensemble] engine {} threw {}: {} - synthesizing failed result",
                engineName, t.getClass().getName(), t.getMessage());
        OcrResultDto failed = new OcrResultDto();
        failed.setSuccess(false);
        failed.setEngineUsed(engineName);
        failed.setErrorMessage(engineName + " failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        return failed;
    }

    private boolean isGood(OcrResultDto r) {
        return r.isSuccess() && r.getExtractedText() != null && r.getExtractedText().length() > 20 && (r.getConfidence() == null || r.getConfidence() >= confThreshold);
    }

    private boolean hasValidNik(OcrResultDto r) {
        if (r.getExtractedText() == null) return false;
        // look for 16 consecutive digits
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b\\d{16}\\b").matcher(r.getExtractedText());
        return m.find();
    }

    private boolean isBetter(OcrResultDto cand, OcrResultDto current) {
        if (!cand.isSuccess() || cand.getExtractedText() == null) return false;
        if (!current.isSuccess()) return true;
        // NIK validity is king for KTP
        boolean candNik = hasValidNik(cand);
        boolean curNik = hasValidNik(current);
        if (candNik != curNik) return candNik;
        int candConf = cand.getConfidence() != null ? cand.getConfidence() : 0;
        int curConf = current.getConfidence() != null ? current.getConfidence() : 0;
        if (candConf != curConf) return candConf > curConf;
        return cand.getExtractedText().length() > current.getExtractedText().length();
    }

    private OcrResultDto withEngine(OcrResultDto r, String engine) {
        r.setEngineUsed(engine);
        return r;
    }
}
