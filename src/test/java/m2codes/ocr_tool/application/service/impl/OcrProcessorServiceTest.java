package m2codes.ocr_tool.application.service.impl;

import jakarta.persistence.EntityManager;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.OcrProcessingAsyncExecutor;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.ocr_tool.application.util.ExtractedTextMapper;
import m2codes.ocr_tool.application.util.TaskManager;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import m2codes.ocr_tool.infrastructure.filemanager.service.FileManagerService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test OcrProcessorService dengan Mockito murni (vis-M4: @SpringBootTest dihapus —
 * sebelumnya empty test yang butuh DB). @PersistenceContext EntityManager di-mock dan
 * di-set via ReflectionTestUtils (field injection di plain unit).
 *
 * UNEXECUTED di sandbox (tanpa javac + MySQL). Jalankan `mvn test` di dev env.
 */
@ExtendWith(MockitoExtension.class)
class OcrProcessorServiceTest {

    @Mock
    private OcrRequestService ocrRequestService;
    @Mock
    private OcrResultService ocrResultService;
    @Mock
    private ExtractedTextService extractedTextService;
    @Mock
    private OcrProcessingAsyncExecutor ocrProcessingAsyncExecutor;
    @Mock
    private TextExtractionService textExtractionService;
    @Mock
    private ExtractedTextCleaner extractedTextCleaner;
    @Mock
    private ExtractedTextMapper extractedTextMapper;
    @Mock
    private TaskManager taskManager;
    @Mock
    private FileManagerService fileManagerService;
    @Mock
    private EntityManager entityManager;

    private OcrProcessorService service;
    private OcrRequest ocrRequest;

    @BeforeEach
    void setUp() {
        service = new OcrProcessorService(
                ocrRequestService,
                ocrResultService,
                extractedTextService,
                ocrProcessingAsyncExecutor,
                textExtractionService,
                extractedTextCleaner,
                extractedTextMapper,
                taskManager,
                fileManagerService);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ocrRequest = OcrRequest.builder()
                .id(UUID.randomUUID())
                .imageUrl("https://web.example/files/ktp.png")
                .build();
    }

    @Test
    void processingExtractionTextSuccessUpdatesRequestToDone() {
        when(entityManager.merge(any(OcrRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        OcrResultDto ocrResultDto = new OcrResultDto();
        ocrResultDto.setSuccess(true);
        ocrResultDto.setExtractedText("nik: 1234567890123456\nnama: TEST NAMA\nalamat: JALAN TEST NO 1");
        when(textExtractionService.extractTextFromImage(anyString(), anyBoolean())).thenReturn(ocrResultDto);

        OcrResult savedResult = new OcrResult();
        when(ocrResultService.save(any(OcrResultDto.class), any(OcrRequest.class))).thenReturn(savedResult);

        // pipeline parse text: cleaner + mapper mocks (proses parse di-return kosong;
        // yang diverifikasi adalah status terminal DONE + persist extractedText path)
        when(extractedTextCleaner.linesCleaner(any(String[].class))).thenReturn(new String[0]);
        when(extractedTextMapper.parseLinesByColon(any(String[].class))).thenReturn(List.of());
        when(extractedTextMapper.detectAndAddMissingKeyValue(any(String[].class), any())).thenReturn(List.of());
        when(extractedTextMapper.filterParsedDataByRequiredKeys(any(List.class), any())).thenReturn(List.of());

        service.processingExtractionText(
                OcrDataRequest.builder().imageUrl("https://web.example/files/ktp.png").build(),
                ocrRequest, "client-a", false);

        verify(ocrRequestService).updateStatus(ocrRequest, RequestStatus.DONE);
        verify(extractedTextService).saveAll(List.of(), savedResult);
    }

    @Test
    void processingExtractionTextEngineExceptionPersistsFailureQuietly() {
        when(entityManager.merge(any(OcrRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        when(textExtractionService.extractTextFromImage(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("engine exploded"));

        // exception TIDAK boleh keluar dari method (async void); FAILURE harus ter-persist
        service.processingExtractionText(
                OcrDataRequest.builder().imageUrl("https://web.example/files/ktp.png").build(),
                ocrRequest, "client-a", false);

        verify(ocrRequestService).updateStatus(ocrRequest, RequestStatus.FAILURE);
    }
}
