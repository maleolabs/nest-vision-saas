package m2codes.perizinan_ocr_tool.application.service.impl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;

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