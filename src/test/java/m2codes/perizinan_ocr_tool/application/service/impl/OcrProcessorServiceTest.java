package m2codes.perizinan_ocr_tool.application.service.impl;

import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.OcrRequestService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
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
    private OcrRequestService ocrRequestService;

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
        String imageUrl = perizinanApiBaseUrl + "/files/MTcyNjA3MzUyNDg1NC1zdXJhdC1wZW5lbGl0aWFuLnBuZw==";
        OcrDataRequest request = OcrDataRequest.builder().imageUrl(imageUrl).izinId(4L).syaratIzinId(855L).jenisPerizinanId(167L).build();
        OcrResultDto ocrResultDto = OcrResultDto.builder().isSuccess(true).extractedText("").build();
        OcrRequest ocrRequest = new OcrRequest();
        OcrResult ocrResult = new OcrResult();

        when(ocrRequestService.save(request)).thenReturn(ocrRequest);
        when(textExtractionService.extractTextFromImage(request.getImageUrl())).thenReturn(ocrResultDto);
        when(ocrResultService.save(ocrResultDto, ocrRequest)).thenReturn(ocrResult);
        doNothing().when(extractedTextService).save(any(), any());

        WebResponse<?> response = concreteOcrProcessor.processingExtractionText(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

}