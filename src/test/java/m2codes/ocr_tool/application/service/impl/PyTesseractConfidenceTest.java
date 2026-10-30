package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.util.ImageQualityAssessor;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import m2codes.ocr_tool.infrastructure.security.util.ImageDownloadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fix conf palsu: PyTesseractOcrService.resolveConfidence harus mengambil
 * confidence dari baris [OCR] ... conf=NN.N di stderr (musuh utama: heuristic
 * result.length()/8 yang jungkir balik memicu fallback ensemble setiap pass).
 */
class PyTesseractConfidenceTest {

    private PyTesseractOcrService newService(PythonOcrExcecutor executor) {
        ImageQualityAssessor qa = new ImageQualityAssessor();
        ImageDownloadValidator dv = new ImageDownloadValidator();
        PyTesseractOcrService s = new PyTesseractOcrService(executor, qa, dv);
        ReflectionTestUtils.setField(s, "imageDownloadValidator", dv);
        return s;
    }

    @Test
    void realConfFromStderrBeatsLengthHeuristic() {
        // KTP1 nyata: teks 226 char. Heuristik lama: 226/8 = 28 (< confThreshold 55 ->
        // fallback menyala). Conf asli python: 73.2 -> isGood, tidak ada fallback.
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        PyTesseractOcrService s = newService(executor);

        String out = "x".repeat(226);
        String err = "[OCR] blur=1766.7 brightness=206.7 contrast=52.9 conf=73.2 psm=3 sr=True persp=False osd=0 deskew=0.0\n";
        int conf = s.resolveConfidence(new PythonOcrExcecutor.OcrScriptResult(out, err));

        assertEquals(73, conf, "confidence harus dari stderr, bukan length/8=28");
        assertTrue(conf >= 55, "conf harus melewati confThreshold ensemble");
    }

    @Test
    void heuristicFallbackWhenMetricsAbsent() {
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        PyTesseractOcrService s = newService(executor);

        int conf = s.resolveConfidence(new PythonOcrExcecutor.OcrScriptResult("x".repeat(226), "no metrics here"));
        assertEquals(28, conf, "fallback heuristik lama ketika [OCR] absent");
    }

    @Test
    void confClampedTo10And95() {
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        PyTesseractOcrService s = newService(executor);

        int low = s.resolveConfidence(new PythonOcrExcecutor.OcrScriptResult("short", "[OCR] conf=2.0 psm=6 sr=False"));
        assertEquals(10, low, "clamp bawah 10");

        String err = "[OCR] blur=1.0 brightness=1.0 contrast=1.0 conf=120.0 psm=6 sr=False";
        int high = s.resolveConfidence(new PythonOcrExcecutor.OcrScriptResult("text", err));
        assertEquals(95, high, "clamp atas 95");
    }
}
