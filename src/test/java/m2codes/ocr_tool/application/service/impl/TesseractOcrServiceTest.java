package m2codes.ocr_tool.application.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author marij_mokoginta
 */
@SpringBootTest
public class TesseractOcrServiceTest {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String perizinanApiBaseUrl;

    @Mock
    private TesseractOcrService tesseractOcrService;
    
    @Test
    public void extractTextFromImageTest() {
        String imageUrl = perizinanApiBaseUrl + "/files/MTcyNTQzMzA1MDg1NS10ZXN0LmpwZw==";

    }

}