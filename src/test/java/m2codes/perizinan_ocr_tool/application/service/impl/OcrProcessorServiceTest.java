package m2codes.perizinan_ocr_tool.application.service.impl;

import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
class OcrProcessorServiceTest {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String perizinanApiBaseUrl;

    @InjectMocks
    private OcrProcessorService concreteOcrProcessor;

    @Mock
    private ImageUploadService imageUploadService;

    @Mock
    private OcrResultService ocrResultService;

    @Mock
    private ExtractedTextService extractedTextService;

    @Mock
    private TextExtractionService textExtractionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testProcessOcr_Success() {
        String imageUrl = perizinanApiBaseUrl + "/files/MTcyNTQzMzA1MDg1NS10ZXN0LmpwZw==";
        ImageUploadRequest request = ImageUploadRequest.builder().imageUrl(imageUrl).izinId(4L).syaratIzinId(855L).jenisPerizinanId(167L).build();
        OcrResultDto ocrResultDto = OcrResultDto.builder().isSuccess(true).extractedText("").build();
        ImageUpload imageUpload = new ImageUpload();
        OcrResult ocrResult = new OcrResult();

        when(imageUploadService.save(request)).thenReturn(imageUpload);
        when(textExtractionService.extractTextFromImage(request.getImageUrl())).thenReturn(ocrResultDto);
        when(ocrResultService.save(ocrResultDto, imageUpload)).thenReturn(ocrResult);
        doNothing().when(extractedTextService).save(any(), any());

        WebResponse<?> response = concreteOcrProcessor.processingExtractionText(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

}