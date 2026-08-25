package m2codes.ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * P2.6 Ensemble: try primary (pyTesseract) -> fallback to native Tesseract -> PaddleOCR
 * Picks best by confidence/heuristics. Also applies NIK whitelist correction logging.
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

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto r1 = guardedExtract(primary, "pytesseract", file, preprocessed);
        log.info("[Ensemble File] primary success={} conf={} len={} blur={}", r1.isSuccess(), r1.getConfidence(), r1.getExtractedText() != null ? r1.getExtractedText().length() : 0, r1.getBlurScore());
        // NIK validity check: if primary has valid 16-digit NIK, treat as good even if conf slightly below threshold
        if (isGood(r1) && hasValidNik(r1)) return withEngine(r1, "pytesseract");
        if (isGood(r1) && r1.getConfidence() != null && r1.getConfidence() >= confThreshold + 10) return withEngine(r1, "pytesseract");

        if (!fallbackEnabled) return r1;

        // native with forced preprocess
        OcrResultDto r2 = guardedExtract(nativeTesseract, "native-tesseract", file, true);
        log.info("[Ensemble File] native conf={} len={}", r2.getConfidence(), r2.getExtractedText() != null ? r2.getExtractedText().length() : 0);

        // rapid (preferred for blur) — try before paddle
        OcrResultDto rRapid = guardedExtract(rapid, "rapid", file, preprocessed);
        log.info("[Ensemble File] rapid success={} conf={} len={} err={}", rRapid.isSuccess(), rRapid.getConfidence(), rRapid.getExtractedText() != null ? rRapid.getExtractedText().length() : 0, rRapid.getErrorMessage());

        // paddle legacy if rapid not good
        OcrResultDto r3 = guardedExtract(paddle, "paddle", file, preprocessed);
        log.info("[Ensemble File] paddle success={} len={} err={}", r3.isSuccess(), r3.getExtractedText() != null ? r3.getExtractedText().length() : 0, r3.getErrorMessage());

        OcrResultDto best = r1;
        if (isBetter(r2, best)) best = withEngine(r2, "native-tesseract");
        if (rRapid.isSuccess() && isBetter(rRapid, best)) best = withEngine(rRapid, "rapid");
        // also consider NIK validity boost: rapid with valid NIK beats everything if others invalid
        if (rRapid.isSuccess() && hasValidNik(rRapid) && !hasValidNik(best)) best = withEngine(rRapid, "rapid");
        if (r3.isSuccess() && isBetter(r3, best)) best = withEngine(r3, "paddle");

        // log observability
        log.info("[Ensemble] chosen engine={} conf={} dur={} best len={} nikValid={}", best.getEngineUsed(), best.getConfidence(), best.getDuration(), best.getExtractedText() != null ? best.getExtractedText().length() : 0, hasValidNik(best));
        return best;
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
