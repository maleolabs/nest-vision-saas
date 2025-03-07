package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.ArgumentMatchers.any;

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