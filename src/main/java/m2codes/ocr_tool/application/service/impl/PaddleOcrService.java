package m2codes.ocr_tool.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * P2.6 Ensemble fallback: PaddleOCR via python.
 * Requires: pip install paddlepaddle paddleocr
 * Script path configurable via ocr.paddle.script.path
 */
@Slf4j
@Service(value = "paddleOcrService")
@RequiredArgsConstructor
public class PaddleOcrService implements TextExtractionService {

    @Value("${ocr.python.path:python3}")
    private String pythonPath;

    @Value("${ocr.paddle.script.path:opt/app/ocr/paddle_ocr.py}")
    private String paddleScriptPath;

    @Value("${ocr.paddle.enabled:false}")
    private boolean enabled;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        // download to temp then delegate to file path
        // for now return error if called with URL (handled by caller fallback)
        OcrResultDto dto = new OcrResultDto();
        dto.setSuccess(false);
        dto.setErrorMessage("PaddleOCR URL not implemented, use file");
        return dto;
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto result = new OcrResultDto();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorMessage("PaddleOCR disabled");
            return result;
        }
        long start = System.currentTimeMillis();
        try {
            String scriptAbs = Paths.get(paddleScriptPath).toAbsolutePath().toString();
            if (!new File(scriptAbs).exists()) {
                result.setSuccess(false);
                result.setErrorMessage("Paddle script not found: " + scriptAbs);
                return result;
            }
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptAbs, file.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) out.append(line).append("\n");
            }
            int exit = p.waitFor();
            if (exit != 0) {
                result.setSuccess(false);
                result.setErrorMessage("Paddle exit " + exit + ": " + out);
                return result;
            }
            result.setExtractedText(out.toString().trim());
            result.setSuccess(true);
            result.setDuration(System.currentTimeMillis() - start);
            result.setEngineUsed("paddle");
            result.setConfidence(75); // Paddle generally higher for blur
            log.info("PaddleOCR success len={} dur={}", result.getExtractedText().length(), result.getDuration());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("PaddleOCR failed", e);
        }
        return result;
    }
}
