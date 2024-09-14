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

    }

}