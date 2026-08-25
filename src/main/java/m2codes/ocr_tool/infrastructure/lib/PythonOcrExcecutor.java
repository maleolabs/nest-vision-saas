package m2codes.ocr_tool.infrastructure.lib;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class PythonOcrExcecutor {

    @Value("${ocr.python.path:python3}")
    private String pythonPath;

    @Value("${ocr.script.path}")
    private String scriptPath;

    public String runOcrScript(String imagePath) throws IOException, InterruptedException {
        String scriptPath = resolveScriptPath(this.scriptPath);

        ProcessBuilder builder = new ProcessBuilder(pythonPath, scriptPath, imagePath);
        builder.redirectErrorStream(false); // separate stderr for observability

        Process process = builder.start();

        String output;
        StringBuilder stderr = new StringBuilder();
        // read stdout
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            output = result.toString();
        }
        // read stderr for [OCR] metrics
        try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errReader.readLine()) != null) {
                stderr.append(line).append("\n");
                // log observability line
                if (line.contains("[OCR]")) {
                    org.slf4j.LoggerFactory.getLogger(PythonOcrExcecutor.class).info(line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code " + exitCode + ". Output: \n" + output + "\nStderr: " + stderr);
        }

        return output.trim();
    }

    private String resolveScriptPath(String configured) {
        // 1. absolute exists
        java.nio.file.Path p = Paths.get(configured);
        if (p.isAbsolute() && java.nio.file.Files.exists(p)) return p.toString();
        // 2. relative to user.dir
        java.nio.file.Path abs = p.toAbsolutePath();
        if (java.nio.file.Files.exists(abs)) return abs.toString();
        // 3. relative to repo root (user.dir may be nested)
        String[] candidates = {
            configured,
            "opt/app/ocr/tesseract_ocr.py",
            "src/main/resources/opt/app/ocr/tesseract_ocr.py"
        };
        String userDir = System.getProperty("user.dir", "");
        for (String c : candidates) {
            java.nio.file.Path cand = Paths.get(userDir, c);
            if (java.nio.file.Files.exists(cand)) return cand.toAbsolutePath().toString();
            // also try parent of userDir (when run from scripts/)
            java.nio.file.Path parentCand = Paths.get(userDir).getParent() != null ? Paths.get(userDir).getParent().resolve(c) : null;
            if (parentCand != null && java.nio.file.Files.exists(parentCand)) return parentCand.toAbsolutePath().toString();
        }
        // fallback: return absolute anyway (will error with clear message)
        return abs.toString();
    }

    public record OcrMetrics(double blur, double brightness, double contrast, double conf, int psm, boolean sr) {}
    public OcrMetrics parseMetrics(String stderr) {
        try {
            // parse "[OCR] blur=... brightness=... contrast=... conf=... psm=... sr=..."
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("blur=([0-9.]+).*brightness=([0-9.]+).*contrast=([0-9.]+).*conf=([0-9.]+).*psm=(\\d+).*sr=(\\w+)").matcher(stderr);
            if (m.find()) {
                return new OcrMetrics(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4)), Integer.parseInt(m.group(5)), Boolean.parseBoolean(m.group(6)));
            }
        } catch (Exception ignored) {}
        return null;
    }

}
