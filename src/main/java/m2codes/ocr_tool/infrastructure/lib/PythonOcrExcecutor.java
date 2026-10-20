package m2codes.ocr_tool.infrastructure.lib;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PythonOcrExcecutor {

    @Value("${ocr.python.path:python3}")
    private String pythonPath;

    @Value("${ocr.script.path}")
    private String scriptPath;

    // VIS-F09: budget keras runtime python — env OCR_PYTHON_TIMEOUT_MILLIS, default 120000.
    // Dibaca langsung dari Environment (pola env-var yang sama seperti OCR_RAPID_ENABLED dst),
    // application.properties tidak diubah. Sebelumnya process hang menahan thread pool
    // OCR (ocrTaskExecutor) selamanya.
    @Value("${OCR_PYTHON_TIMEOUT_MILLIS:120000}")
    private long timeoutMillis;

    private static final int DESTROY_GRACE_MILLIS = 2_000;
    private static final int READER_JOIN_MILLIS = 2_000;

    public String runOcrScript(String imagePath) throws IOException, InterruptedException {
        String scriptPath = resolveScriptPath(this.scriptPath);

        ProcessBuilder builder = new ProcessBuilder(pythonPath, scriptPath, imagePath);
        builder.redirectErrorStream(false); // separate stderr for observability

        Process process = builder.start();

        // VIS-F09: kedua stream dikonsumsi dari thread reader sejak awal supaya proses tidak
        // deadlock menulis pipe penuh sementara kita menunggu bounded waitFor. Reader daemon
        // berhenti sendiri via EOF ketika process exit (normal maupun destroy) —tidak ada leak.
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        Thread stdoutReader = startReaderThread(process.getInputStream(), stdout);
        Thread stderrReader = startReaderThread(process.getErrorStream(), stderr);

        boolean finished;
        try {
            finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw e;
        }

        if (!finished) {
            destroyGracefully(process);
            log.warn("OCR python process timed out after {} ms", timeoutMillis);
            // pesan generik — tidak bocorkan detail internal; caller (PyTesseractOcrService)
            // menangkap Exception dan menaruh getMessage() ke OcrResultDto.errorMessage
            throw new RuntimeException("OCR processing timed out");
        }

        // process sudah exit → streams EOF → reader berhenti; join pendek defensive
        awaitReader(stdoutReader);
        awaitReader(stderrReader);

        String output = stdout.toString();
        String stderrValue = stderr.toString();

        int exitCode = process.waitFor(); // reap; process sudah pasti exit di titik ini
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code " + exitCode + ". Output: \n" + output + "\nStderr: \n" + stderrValue);
        }

        return output.trim();
    }

    private Thread startReaderThread(InputStream in, StringBuilder target) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    target.append(line).append('\n');
                    // log observability line
                    if (line.contains("[OCR]")) {
                        log.info(line);
                    }
                }
            } catch (IOException ignored) {
                // stream di-close via destroy (timeout) — reader keluar diam-diam
            }
        }, "ocr-python-output-reader");
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

    private void awaitReader(Thread reader) {
        try {
            reader.join(READER_JOIN_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * destroy() (SIGTERM) dulu supaya python sempat cleanup, lalu destroyForcibly()
     * (SIGKILL) kalau tidak mati dalam grace window. File output OCR TIDAK dihapus
     * di sini — penghapusan temp tetap tanggung jawab caller setelah result dipakai.
     */
    private void destroyGracefully(Process process) {
        process.destroy();
        boolean reaped = false;
        try {
            reaped = process.waitFor(DESTROY_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!reaped) {
            process.destroyForcibly();
        }
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
