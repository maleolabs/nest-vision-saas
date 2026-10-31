package m2codes.ocr_tool.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ImageQualityAssessor;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import m2codes.ocr_tool.infrastructure.security.util.ImageDownloadValidator;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

@Slf4j
@Service(value = "pyTesseractService")
@RequiredArgsConstructor
public class PyTesseractOcrService implements TextExtractionService {

    private final PythonOcrExcecutor excecutor;
    private final ImageQualityAssessor qualityAssessor;
    // S-07/VIS-F08: satu SSRF guard untuk semua fetch by URL
    private final ImageDownloadValidator imageDownloadValidator;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto ocrResult = new OcrResultDto();

        try {
            // SSRF guard + controlled fetch (menggantikan ImageIO.read(new URL(imageUrl)))
            byte[] imageBytes = imageDownloadValidator.download(imageUrl);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            if (image == null) {
                ocrResult.setSuccess(false);
                ocrResult.setErrorMessage("Failed to read image from URL");
                return ocrResult;
            }

            File tempFile = File.createTempFile("ocr_input_", ".png");
            ImageIO.write(image, "png", tempFile);

            long startTime = System.currentTimeMillis();

            OcrResultDto pythonResult = runPython(tempFile);

            long duration = System.currentTimeMillis() - startTime;

            pythonResult.setDuration(duration);
            pythonResult.setEngineUsed("pytesseract");
            if (tempFile.exists()) tempFile.delete();
            return pythonResult;
        } catch (Exception e) {
            ocrResult.setSuccess(false);
            ocrResult.setErrorMessage(e.getMessage());
            log.error("pyTesseract URL failed", e);
        }

        return ocrResult;
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto ocrResult = new OcrResultDto();

        try {
            long startTime = System.currentTimeMillis();

            // Note: preprocessed flag is now handled inside python's preprocess_image_v2
            // but we log it; python does full pipeline regardless, but flag controls SR trigger
            log.info("pyTesseract file {} preprocessed={}", file.getName(), preprocessed);

            ocrResult = runPython(file);

            ocrResult.setDuration(System.currentTimeMillis() - startTime);
            ocrResult.setEngineUsed("pytesseract");
        } catch (InterruptedException e) {
            // Pembatalan normal oleh ensemble early-exit: engine lain sudah menang,
            // virtual thread di-cancel (f.cancel(true)) -> waitFor melempar interrupt.
            // PythonOcrExcecutor sudah destroyForcibly proses python di titik ini.
            // Bukan error — log ringan tanpa stack trace supaya tidak menyesatkan.
            Thread.currentThread().interrupt();
            ocrResult.setSuccess(false);
            ocrResult.setEngineUsed("pytesseract");
            ocrResult.setErrorMessage("pytesseract cancelled by ensemble early-exit");
            log.warn("pyTesseract cancelled (ensemble early-exit), python process destroyed");
        } catch (Exception e) {
            ocrResult.setSuccess(false);
            ocrResult.setErrorMessage(e.getMessage());
            log.error("pyTesseract file failed", e);
        }

        return ocrResult;
    }

    /**
     * Jalankan script OCR python dan ambil confidence ASLI dari baris
     * observability stderr `[OCR] ... conf=NN.N`.
     *
     * Sebelumnya confidence dipalsukan dari panjang teks
     * (result.length()/8), yang membuatnya hampir selalu jatuh di bawah
     * conf-threshold ensemble (55) dan memicu fallback engine lain pada
     * setiap pass. Bila stderr tak ada baris [OCR] (script lama), jatuh
     * kembali ke heuristik panjang teks demi kompatibilitas.
     */
    private OcrResultDto runPython(File file) throws Exception {
        PythonOcrExcecutor.OcrScriptResult res = excecutor.runOcrScriptWithStderr(file.getAbsolutePath());

        OcrResultDto ocrResult = new OcrResultDto();
        ocrResult.setSuccess(true);
        ocrResult.setExtractedText(res.stdout());
        ocrResult.setConfidence(resolveConfidence(res));
        fillMetrics(ocrResult, res);
        return ocrResult;
    }

    /**
     * Isi metrik kualitas gambar (blur/brightness/contrast/psm/sr) ke DTO.
     * Sebelumnya parseMetrics hanya dipakai untuk confidence — metrik lain
     * sudah di-parse dari baris [OCR] stderr tapi dibuang, sehingga kolom
     * statistik di DB (V10) selalu NULL untuk engine ini.
     */
    protected void fillMetrics(OcrResultDto dto, PythonOcrExcecutor.OcrScriptResult res) {
        PythonOcrExcecutor.OcrMetrics metrics = excecutor.parseMetrics(res.stderr());
        if (metrics == null) return;
        dto.setBlurScore(metrics.blur());
        dto.setBrightness(metrics.brightness());
        dto.setContrast(metrics.contrast());
        dto.setPsmUsed(String.valueOf(metrics.psm()));
        dto.setSuperResolutionApplied(metrics.sr());
    }

    /** protected untuk unit test tanpa menjalankan process python. */
    protected int resolveConfidence(PythonOcrExcecutor.OcrScriptResult res) {
        PythonOcrExcecutor.OcrMetrics metrics = excecutor.parseMetrics(res.stderr());
        if (metrics != null) {
            int conf = (int) Math.min(95, Math.max(10, metrics.conf()));
            log.info("pyTesseract real conf={} blur={} psm={}", metrics.conf(), metrics.blur(), metrics.psm());
            return conf;
        }
        int heuristic = Math.min(95, Math.max(10, res.stdout().length() / 8));
        log.info("pyTesseract stderr metrics absent, heuristic conf={}", heuristic);
        return heuristic;
    }

}
