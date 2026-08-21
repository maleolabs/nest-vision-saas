package m2codes.ocr_tool.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ImageQualityAssessor;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

@Slf4j
@Service(value = "pyTesseractService")
@RequiredArgsConstructor
public class PyTesseractOcrService implements TextExtractionService {

    private final PythonOcrExcecutor excecutor;
    private final ImageQualityAssessor qualityAssessor;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto ocrResult = new OcrResultDto();

        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));

            if (image == null) {
                ocrResult.setSuccess(false);
                ocrResult.setErrorMessage("Failed to read image from URL");
                return ocrResult;
            }

            // P2.5 auto quality gate if not already preprocessed
            if (!preprocessed) {
                try {
                    // quick blur estimate via image size + will be refined in python
                    log.info("pyTesseract URL preprocessed={} - python will auto-assess quality", preprocessed);
                } catch (Exception ignored) {}
            }

            File tempFile = File.createTempFile("ocr_input_", ".png");
            ImageIO.write(image, "png", tempFile);

            long startTime = System.currentTimeMillis();

            String result = excecutor.runOcrScript(tempFile.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;

            ocrResult.setSuccess(true);
            ocrResult.setExtractedText(result);
            ocrResult.setDuration(duration);
            ocrResult.setEngineUsed("pytesseract");
            // try parse metrics from stderr if available (python script logs to stderr)
            // for URL path we don't have stderr captured in this overload - confidence heuristic
            ocrResult.setConfidence(Math.min(95, Math.max(10, result.length() / 10)));
            if (tempFile.exists()) tempFile.delete();
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

            String result = excecutor.runOcrScript(file.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;

            ocrResult.setExtractedText(result);
            ocrResult.setDuration(duration);
            ocrResult.setSuccess(true);
            ocrResult.setEngineUsed("pytesseract");
            ocrResult.setConfidence(Math.min(95, Math.max(10, result.length() / 8)));
            // blur/quality will be logged via PythonOcrExcecutor stderr parsing in future
        } catch (Exception e) {
            ocrResult.setSuccess(false);
            ocrResult.setErrorMessage(e.getMessage());
            log.error("pyTesseract file failed", e);
        }

        return ocrResult;
    }

}
