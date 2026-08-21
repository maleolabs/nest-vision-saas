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

    @Value("${ocr.ensemble.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${ocr.ensemble.conf-threshold:60}")
    private int confThreshold;

    public EnsembleOcrService(
            @Qualifier("pyTesseractService") TextExtractionService primary,
            @Qualifier("nativeTesseractService") TextExtractionService nativeTesseract,
            @Qualifier("paddleOcrService") TextExtractionService paddle) {
        this.primary = primary;
        this.nativeTesseract = nativeTesseract;
        this.paddle = paddle;
    }

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto r1 = primary.extractTextFromImage(imageUrl, preprocessed);
        log.info("[Ensemble URL] primary success={} conf={} len={}", r1.isSuccess(), r1.getConfidence(), r1.getExtractedText() != null ? r1.getExtractedText().length() : 0);
        if (isGood(r1)) return withEngine(r1, "pytesseract");

        if (!fallbackEnabled) return r1;

        // try native with preprocess forced
        log.info("[Ensemble URL] trying native fallback");
        OcrResultDto r2 = nativeTesseract.extractTextFromImage(imageUrl, true);
        if (isBetter(r2, r1)) {
            log.info("[Ensemble URL] native wins conf={}", r2.getConfidence());
            return withEngine(r2, "native-tesseract");
        }
        return withEngine(r1, "pytesseract");
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto r1 = primary.extractTextFromImage(file, preprocessed);
        log.info("[Ensemble File] primary success={} conf={} len={} blur={}", r1.isSuccess(), r1.getConfidence(), r1.getExtractedText() != null ? r1.getExtractedText().length() : 0, r1.getBlurScore());
        if (isGood(r1)) return withEngine(r1, "pytesseract");

        if (!fallbackEnabled) return r1;

        // native with forced preprocess
        OcrResultDto r2 = nativeTesseract.extractTextFromImage(file, true);
        log.info("[Ensemble File] native conf={} len={}", r2.getConfidence(), r2.getExtractedText() != null ? r2.getExtractedText().length() : 0);

        // paddle if enabled and both low
        OcrResultDto r3 = paddle.extractTextFromImage(file, preprocessed);
        log.info("[Ensemble File] paddle success={} len={}", r3.isSuccess(), r3.getExtractedText() != null ? r3.getExtractedText().length() : 0);

        OcrResultDto best = r1;
        if (isBetter(r2, best)) best = withEngine(r2, "native-tesseract");
        if (r3.isSuccess() && isBetter(r3, best)) best = withEngine(r3, "paddle");

        // log observability
        log.info("[Ensemble] chosen engine={} conf={} dur={} best len={}", best.getEngineUsed(), best.getConfidence(), best.getDuration(), best.getExtractedText() != null ? best.getExtractedText().length() : 0);
        return best;
    }

    private boolean isGood(OcrResultDto r) {
        return r.isSuccess() && r.getExtractedText() != null && r.getExtractedText().length() > 20 && (r.getConfidence() == null || r.getConfidence() >= confThreshold);
    }

    private boolean isBetter(OcrResultDto cand, OcrResultDto current) {
        if (!cand.isSuccess() || cand.getExtractedText() == null) return false;
        if (!current.isSuccess()) return true;
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
