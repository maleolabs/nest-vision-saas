package m2codes.perizinan_ocr_tool.infrastucture.ocr;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.infrastructure.ocr.TesseractOcrService;

/**
 *
 * @author marij_mokoginta
 */
@SpringBootTest
public class TesseractOcrServiceTest {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String perizinanApiBaseUrl;

    @Autowired
    private TesseractOcrService tesseractOcrService;
    
    @Test
    public void extractTextFromImageTest() {
        String imageUrl = perizinanApiBaseUrl + "/files/MTcyNTQzMzA1MDg1NS10ZXN0LmpwZw==";
        OcrResultDto result = tesseractOcrService.extractTextFromImage(imageUrl);

        List<ExtractedTextDto> extractedTexts = tesseractOcrService.extractKeyValueFromText(result.getExtractedText());

        extractedTexts.forEach(extractedText -> {
            System.out.println(extractedText.getTextKey() + "-> " + extractedText.getTextValue());
        });
    }

}