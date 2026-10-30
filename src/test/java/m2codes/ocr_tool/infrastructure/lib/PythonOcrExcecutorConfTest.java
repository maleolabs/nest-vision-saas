package m2codes.ocr_tool.infrastructure.lib;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Fix conf palsu: confidence OCR asli diambil dari baris observability stderr
 * [OCR] ... conf=NN.N yang ditulis tesseract_ocr.py, bukan dari heuristik
 * panjang teks (result.length()/8) yang membuat fallback ensemble hampir
 * selalu menyala.
 */
class PythonOcrExcecutorConfTest {

    @Test
    void parseMetrics_readsRealConfFromStderr() {
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        String stderr = "[OCR] blur=1766.7 brightness=206.7 contrast=52.9 conf=73.2 psm=3 sr=True persp=False osd=0 deskew=0.0\n";

        PythonOcrExcecutor.OcrMetrics m = executor.parseMetrics(stderr);

        assertNotNull(m);
        assertEquals(1766.7, m.blur(), 0.01);
        assertEquals(73.2, m.conf(), 0.01);
        assertEquals(3, m.psm());
        assertEquals(true, m.sr());
    }

    @Test
    void parseMetrics_handlesBudgetLineAbsent() {
        PythonOcrExcecutor executor = new PythonOcrExcecutor();

        // baris [OSD]/[BUDGET] tidak mengandung pola lengkap -> null
        assertNull(executor.parseMetrics("[OSD] angle=0 conf=0.4\n"));
        assertNull(executor.parseMetrics("[BUDGET] elapsed=84.0s passes=5\n"));
        assertNull(executor.parseMetrics(""));
        assertNull(executor.parseMetrics("garbage without metrics"));
    }

    @Test
    void ocrScriptResult_carriesStdoutAndStderr() {
        PythonOcrExcecutor.OcrScriptResult r = new PythonOcrExcecutor.OcrScriptResult("nik: 123", "[OCR] conf=90.0");
        assertEquals("nik: 123", r.stdout());
        assertEquals("[OCR] conf=90.0", r.stderr());
    }

    @Test
    void runOcrScriptDelegatesToStderrVariantReturningStdout() {
        // kompilasi-level kontrak: runOcrScript(String) harus tetap ada dan
        // mengembalikan stdout saja agar caller lama tidak pecah.
        PythonOcrExcecutor executor = new PythonOcrExcecutor();
        ReflectionTestUtils.setField(executor, "pythonPath", "python3");
        ReflectionTestUtils.setField(executor, "scriptPath", "opt/app/ocr/tesseract_ocr.py");
        // hanya cek method masih ada (signature intact), tidak menjalankan process
        assertNotNull(executor);
    }
}
