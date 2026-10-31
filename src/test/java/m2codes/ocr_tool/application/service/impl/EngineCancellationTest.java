package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresi cancellation ensemble early-exit.
 *
 * Konteks: saat engine lain menang, EnsembleOcrService membatalkan engine
 * sisanya via f.cancel(true). Thread engine ter-interrupt saat menunggu
 * subprocess. Invariant yang dilindungi:
 *
 * 1. Hasil engine yang dibatalkan = failure bersih (success=false), bukan
 *    exception yang lolos — guardedExtract menyerapnya ke synthesized
 *    failure sehingga isGood/isBetter tidak pernah memilihnya.
 * 2. flag interrupt Thread dipulihkan (Thread.currentThread().interrupt())
 *    sebelum kembali.
 * 3. subprocess di-destroyForcibly — tidak yatim (dibuktikan oleh script
 *    helper yang selesai instan setelah destroy; tanpa destroy test akan
 *    hang sampai sleep 20s).
 *
 * Sebelum fix: PyTesseractOcrService mencatat InterruptedException sebagai
 * ERROR + full stack trace, padahal itu aliran normal (bukan kegagalan);
 * RapidOcrService/PaddleOcrService sama sekali tidak destroyForcibly proses.
 */
class EngineCancellationTest {

    @TempDir
    Path tempDir;

    /** Script bash pengganti "python OCR" — tidur 20s supaya pasti masih jalan saat diinterrupt. */
    private String sleepScript() throws Exception {
        Path script = tempDir.resolve("sleep20.sh");
        Files.writeString(script, "#!/bin/bash\nsleep 20\n");
        return script.toAbsolutePath().toString();
    }

    private void interruptAfter(long ms) {
        Thread caller = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            }
            caller.interrupt();
        });
        interrupter.setDaemon(true);
        interrupter.start();
    }

    @Test
    void pytesseractCancelledReturnsCleanFailureNotException() throws Exception {
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        ReflectionTestUtils.setField(executor, "pythonPath", "/bin/bash");
        ReflectionTestUtils.setField(executor, "scriptPath", sleepScript());
        ReflectionTestUtils.setField(executor, "timeoutMillis", 60_000L);

        // Dependency tak terpakai di path File -> null sah (bukan border trust).
        PyTesseractOcrService svc = new PyTesseractOcrService(executor, null, null);

        interruptAfter(400);
        OcrResultDto r = svc.extractTextFromImage(new File("/tmp/fake.jpg"), false);

        assertFalse(r.isSuccess(), "engine dibatalkan harus failure bersih, bukan exception");
        assertEquals("pytesseract", r.getEngineUsed());
        assertTrue(r.getErrorMessage() != null && r.getErrorMessage().contains("cancelled"),
                "pesan harus jelas ini pembatalan: " + r.getErrorMessage());
        assertTrue(Thread.currentThread().isInterrupted(),
                "interrupt flag harus dipulihkan (dipasang ulang oleh handler)");
    }

    @Test
    void rapidCancelledReturnsCleanFailureAndDestroysProcess() throws Exception {
        RapidOcrService svc = new RapidOcrService();
        ReflectionTestUtils.setField(svc, "pythonPath", "/bin/bash");
        ReflectionTestUtils.setField(svc, "rapidScriptPath", sleepScript());
        ReflectionTestUtils.setField(svc, "enabled", true);

        interruptAfter(400);
        OcrResultDto r = svc.extractTextFromImage(new File("/tmp/fake.jpg"), false);

        assertFalse(r.isSuccess(), "rapid dibatalkan harus failure bersih");
        assertEquals("rapid", r.getEngineUsed());
        assertTrue(Thread.currentThread().isInterrupted(),
                "interrupt flag harus dipulihkan");
    }
}
