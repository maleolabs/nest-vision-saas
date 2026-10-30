package m2codes.ocr_tool.application.service.impl;

import jakarta.persistence.EntityManager;
import m2codes.ocr_tool.application.service.OcrProcessingAsyncExecutor;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.ocr_tool.application.util.ExtractedTextMapper;
import m2codes.ocr_tool.application.util.TaskManager;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import m2codes.ocr_tool.infrastructure.filemanager.service.FileManagerService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test untuk bug Objenesis + method entry point {@code final} (VIS-F01 follow-up).
 *
 * <p>Spring mem-proxy {@link OcrProcessorService} dengan CGLIB (@Async/@Transactional) dan
 * meng-instansiasi proxy lewat Objenesis <b>tanpa</b> memanggil constructor, sehingga field
 * dependency yang di-inherit dari {@code TextProcessorService} bernilai null di instance proxy.
 * Selama entry point-nya {@code final}, CGLIB tidak bisa meng-override → pemanggilan lewat
 * proxy mengeksekusi implementasi base pada instance proxy → NPE di dispatch ke executor.
 *
 * <p>Test ini membuat proxy CGLIB nyata via {@link ProxyFactory} (bukan Spring context, jadi
 * tidak butuh DB) dan membuktikan pemanggilan lewat proxy mendelegasikan ke target bean.
 */
class OcrProcessorServiceProxyTest {

    private static final String CLIENT_ID = "client-a";
    private static final String IMAGE_URL = "https://web.example/files/ktp.png";

    private OcrRequestService ocrRequestService;
    private OcrResultService ocrResultService;
    private ExtractedTextService extractedTextService;
    private OcrProcessingAsyncExecutor ocrProcessingAsyncExecutor;
    private TextExtractionService textExtractionService;
    private ExtractedTextCleaner extractedTextCleaner;
    private ExtractedTextMapper extractedTextMapper;
    private TaskManager taskManager;
    private FileManagerService fileManagerService;

    private OcrProcessorService target;
    private OcrProcessorService proxy;

    @BeforeEach
    void setUp() {
        ocrRequestService = mock(OcrRequestService.class);
        ocrResultService = mock(OcrResultService.class);
        extractedTextService = mock(ExtractedTextService.class);
        ocrProcessingAsyncExecutor = mock(OcrProcessingAsyncExecutor.class);
        textExtractionService = mock(TextExtractionService.class);
        extractedTextCleaner = mock(ExtractedTextCleaner.class);
        extractedTextMapper = mock(ExtractedTextMapper.class);
        taskManager = mock(TaskManager.class);
        fileManagerService = mock(FileManagerService.class);

        target = new OcrProcessorService(
                ocrRequestService,
                ocrResultService,
                extractedTextService,
                ocrProcessingAsyncExecutor,
                textExtractionService,
                extractedTextCleaner,
                extractedTextMapper,
                taskManager,
                fileManagerService);
        ReflectionTestUtils.setField(target, "entityManager", mock(EntityManager.class));

        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxy = (OcrProcessorService) proxyFactory.getProxy();
    }

    @Test
    void cglibProxyIsInstantiatedWithoutConstructor() throws Exception {
        // Prasyarat bug: proxy CGLIB dibuat Objenesis tanpa constructor, jadi dependency
        // warisan TextProcessorService null di instance proxy. Kalau Spring suatu saat
        // berhenti melakukan ini, test ini yang pertama memberi tahu (precondition berubah).
        // Catatan: pakai field access langsung — ReflectionTestUtils.getField otomatis
        // meng-unwrap AOP proxy ke target bean, sehingga tidak bisa membuktikan ini.
        assertThat(proxy).isNotSameAs(target);
        assertThat(proxy.getClass().getName()).contains("CGLIB");
        assertThat(readField(proxy, "ocrProcessingAsyncExecutor")).isNull();
        assertThat(readField(target, "ocrProcessingAsyncExecutor")).isSameAs(ocrProcessingAsyncExecutor);
    }

    @Test
    void imageUrlEntryPointDelegatesToTargetAndDispatchesAsyncWork() {
        OcrRequest saved = OcrRequest.builder()
                .id(UUID.randomUUID())
                .imageUrl(IMAGE_URL)
                .status(RequestStatus.PROCESSING)
                .build();
        when(taskManager.isPoolAvailable()).thenReturn(true);
        when(ocrRequestService.save(any(OcrDataRequest.class), eq(RequestStatus.PROCESSING), eq(CLIENT_ID)))
                .thenReturn(saved);

        ApiResponse<?> response = assertDoesNotThrow(
                () -> proxy.processOcrRequestWithImageUrl(
                        OcrDataRequest.builder().imageUrl(IMAGE_URL).build(), CLIENT_ID),
                "entry point final membuat CGLIB menjalankan method pada instance proxy (dependency null) -> NPE");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isInstanceOf(OcrResponse.class);
        assertThat(((OcrResponse) response.getData()).getRequestId()).isEqualTo(saved.getId().toString());

        // Bukti delegasi ke target + @Async tetap lewat executor (bukan self-invocation).
        verify(ocrProcessingAsyncExecutor)
                .dispatchUrlProcessing(any(OcrDataRequest.class), same(saved), eq(CLIENT_ID), eq(false));
    }

    @Test
    void uploadedImageEntryPointDelegatesToTargetAndDispatchesAsyncWork() throws Exception {
        MultipartFile image = mock(MultipartFile.class);
        File tempFile = new File("ktp-upload-test.png");
        OcrRequest saved = OcrRequest.builder()
                .id(UUID.randomUUID())
                .imageUrl("uploaded-ktp.png")
                .status(RequestStatus.WAITING)
                .build();
        when(taskManager.isPoolAvailable()).thenReturn(false);
        when(fileManagerService.uploadFile(image)).thenReturn("uploaded-ktp.png");
        when(fileManagerService.retrieveFile("uploaded-ktp.png"))
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        when(fileManagerService.createTempFileFromInputStream(any(), anyString())).thenReturn(tempFile);
        when(ocrRequestService.save(any(OcrDataRequest.class), eq(RequestStatus.WAITING), eq(CLIENT_ID)))
                .thenReturn(saved);

        ApiResponse<?> response = assertDoesNotThrow(
                () -> proxy.processOcrRequestWithUploadedImage(
                        OcrDataRequest.builder().image(image).build(), CLIENT_ID),
                "jalur upload adalah lokasi NPE dispatchFileProcessing sebelum fix");

        assertThat(response.isSuccess()).isTrue();
        verify(ocrProcessingAsyncExecutor)
                .dispatchFileProcessing(same(tempFile), same(saved), any(OcrDataRequest.class), eq(CLIENT_ID), eq(false));
    }

    /**
     * Baca field langsung dari instance (tanpa unwrap AOP proxy seperti
     * {@link ReflectionTestUtils#getField(Object, String)}).
     */
    private static Object readField(Object bean, String name) throws Exception {
        Class<?> type = bean.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(bean);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
