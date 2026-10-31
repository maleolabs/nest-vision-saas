package m2codes.ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
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

    /**
     * Dipakai untuk parse baris [OCR] ... yang sekarang dipancarkan rapid_ocr.py
     * (redirectErrorStream(true) menyatukan stderr ke stdout jadi terbaca di out).
     */
    @Autowired
    private PythonOcrExcecutor executor;

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
            String scriptAbs = resolveScriptPath(rapidScriptPath);
            if (!new File(scriptAbs).exists()) {
                result.setSuccess(false);
                result.setErrorMessage("Rapid script not found: " + scriptAbs + " (configured=" + rapidScriptPath + ", cwd=" + System.getProperty("user.dir") + ")");
                log.warn(result.getErrorMessage());
                return result;
            }
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptAbs, file.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            // Reader thread daemon + waitFor: readLine() blocking I/O TIDAK responsif
            // terhadap interrupt (Linux), jadi baca stream tidak boleh menahan thread
            // pemanggil. Pola ini sama dengan PythonOcrExcecutor — saat interrupt,
            // waitFor() melempar, kita destroyForcibly (stream EOF -> reader keluar).
            Thread reader = startOutputReader(p, out);
            int exit;
            try {
                exit = p.waitFor();
            } catch (InterruptedException ie) {
                // Dibatalkan ensemble early-exit (engine lain menang). Bunuh proses
                // python supaya tidak yatim.
                Thread.currentThread().interrupt();
                p.destroyForcibly();
                result.setSuccess(false);
                result.setEngineUsed("rapid");
                result.setErrorMessage("rapid cancelled by ensemble early-exit");
                log.warn("RapidOCR cancelled (ensemble early-exit), python process destroyed");
                return result;
            }
            awaitReader(reader);
            if (exit != 0) {
                result.setSuccess(false);
                result.setErrorMessage("Rapid exit " + exit + ": " + out);
                return result;
            }
            String text = out.toString().trim();
            // rapid_ocr.py memancarkan baris [OCR] ... lewat stderr yang
            // redirectErrorStream(true) satukan ke stdout. Strip baris itu dari
            // teks hasil supaya tidak bocor ke output OCR, lalu parse metriknya.
            PythonOcrExcecutor.OcrMetrics metrics = null;
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("^\\[OCR\\] .*$", java.util.regex.Pattern.MULTILINE).matcher(text);
            if (m.find()) {
                metrics = executor.parseMetrics(m.group(0));
                text = m.replaceAll("").trim();
            }
            result.setExtractedText(text);
            result.setSuccess(true);
            result.setDuration(System.currentTimeMillis() - start);
            result.setEngineUsed("rapid");
            if (metrics != null) {
                result.setBlurScore(metrics.blur());
                result.setBrightness(metrics.brightness());
                result.setContrast(metrics.contrast());
                result.setPsmUsed(String.valueOf(metrics.psm()));
                result.setSuperResolutionApplied(metrics.sr());
            }
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

    private Thread startOutputReader(Process p, StringBuilder out) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) out.append(line).append("\n");
            } catch (IOException ignored) {
                // stream di-close via destroy — reader keluar diam-diam
            }
        }, "rapid-output-reader");
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

    private void awaitReader(Thread reader) {
        try {
            reader.join(2_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private String resolveScriptPath(String configured) {
        java.nio.file.Path p = Paths.get(configured);
        if (p.isAbsolute() && java.nio.file.Files.exists(p)) return p.toString();
        java.nio.file.Path abs = p.toAbsolutePath();
        if (java.nio.file.Files.exists(abs)) return abs.toString();
        String[] candidates = { configured, "opt/app/ocr/rapid_ocr.py", "opt/app/ocr/tesseract_ocr.py" };
        String userDir = System.getProperty("user.dir", "");
        for (String c : candidates) {
            java.nio.file.Path cand = Paths.get(userDir, c);
            if (java.nio.file.Files.exists(cand)) return cand.toAbsolutePath().toString();
            java.nio.file.Path parentCand = Paths.get(userDir).getParent() != null ? Paths.get(userDir).getParent().resolve(c) : null;
            if (parentCand != null && java.nio.file.Files.exists(parentCand)) return parentCand.toAbsolutePath().toString();
        }
        return abs.toString();
    }
}
