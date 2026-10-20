package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.util.ImageQualityAssessor;
import m2codes.ocr_tool.infrastructure.security.util.ImageDownloadValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit test murni jalur-rejection URL TesseractOcrService (enforcement S-07/VIS-F08),
 * menggantikan empty @SpringBootTest lama (vis-M4: butuh DB, tidak ada test body).
 *
 * UNEXECUTED di sandbox (tanpa javac). Jalankan `mvn test` di dev env.
 */
@ExtendWith(MockitoExtension.class)
class TesseractOcrServiceTest {

    @Mock
    private ImageQualityAssessor qualityAssessor;

    @Mock
    private ImageDownloadValidator imageDownloadValidator;

    @Test
    void extractTextFromImageUrlRejectsBlockedHostAsFailedResult() {
        // ImageDownloadValidator di-MOCK supaya TIDAK butuh DNS/network;
        // yang diverifikasi: rejection ditranslasikan menjadi OcrResultDto gagal
        // (success=false) via path createErrorResult "Failed to load image: ..."
        when(imageDownloadValidator.download(anyString()))
                .thenThrow(new IllegalArgumentException("Image URL host is not allowed"));

        TesseractOcrService service = new TesseractOcrService(qualityAssessor, imageDownloadValidator);
        OcrResultDto result = service.extractTextFromImage("https://trusted.example.invalid/img.png", false);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Failed to load image"));
    }
}
