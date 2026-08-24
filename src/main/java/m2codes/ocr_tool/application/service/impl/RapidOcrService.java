package m2codes.ocr_tool.application.service.impl;

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
 * E: RapidOCR ONNX fallback — lightweight alternative to Paddle.
 * Requires: pip install rapidocr_onnxruntime pyclipper shapely onnxruntime
 * Script path configurable via ocr.rapid.script.path
 */
@Slf4j
@Service(value = "rapidOcrService")
public class RapidOcrService implements TextExtractionService {

    @Value("${ocr.python.path:python3}")
    private String pythonPath;

    @Value("${ocr.rapid.script.path:opt/app/ocr/rapid_ocr.py}")
    private String rapidScriptPath;

    @Value("${ocr.rapid.enabled:true}")
    private boolean enabled;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto dto = new OcrResultDto();
        dto.setSuccess(false);
        dto.setErrorMessage("RapidOCR URL not implemented, use file");
        return dto;
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto result = new OcrResultDto();
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorMessage("RapidOCR disabled");
            return result;
        }
        long start = System.currentTimeMillis();
        try {
            String scriptAbs = Paths.get(rapidScriptPath).toAbsolutePath().toString();
            if (!new File(scriptAbs).exists()) {
                result.setSuccess(false);
                result.setErrorMessage("Rapid script not found: " + scriptAbs);
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
                result.setErrorMessage("Rapid exit " + exit + ": " + out);
                return result;
            }
            String text = out.toString().trim();
            result.setExtractedText(text);
            result.setSuccess(true);
            result.setDuration(System.currentTimeMillis() - start);
            result.setEngineUsed("rapid");
            // heuristic confidence: if NIK 16 digits present, high confidence
            int conf = 70;
            if (text.contains("nik:") && text.matches("(?s).*nik:\\s*\\d{16}.*")) conf = 85;
            else if (text.length() > 100) conf = 75;
            result.setConfidence(conf);
            log.info("RapidOCR success len={} dur={} conf={}", text.length(), result.getDuration(), conf);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("RapidOCR failed", e);
        }
        return result;
    }
}
