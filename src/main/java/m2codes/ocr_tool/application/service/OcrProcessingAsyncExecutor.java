package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.application.service.impl.OcrProcessorService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * VIS-F01: bean terpisah untuk men-trigger method @Async di OcrProcessorService.
 * Sebelumnya TextProcessorService memanggil processingExtractionText() via this,
 * sehingga proxy @Async di-bypass (self-invocation) dan OCR berjalan on-thread HTTP
 * (blocking hingga 60 s, thread exhaustion).
 *
 * @Lazy memutus siklus konstruktor tanpa nullable dependency atau lookup runtime.
 * Reference tetap constructor-injected dan Spring fail-fast bila target bean tidak ada.
 */
@Component
public class OcrProcessingAsyncExecutor {

    private final OcrProcessorService ocrProcessorService;

    public OcrProcessingAsyncExecutor(@Lazy OcrProcessorService ocrProcessorService) {
        this.ocrProcessorService = ocrProcessorService;
    }

    /**
     * Reject (pool + queue penuh, lihat AppConfig RejectedExecutionHandler) dilempar
     * keluar ke @ControllerAdvice (ErrorController) sehingga client menerima 503,
     * bukan fake-success.
     */
    public void dispatchUrlProcessing(OcrDataRequest request, OcrRequest ocrRequest, String clientId, boolean preprocessed) {
        ocrProcessorService.processingExtractionText(request, ocrRequest, clientId, preprocessed);
    }

    public void dispatchFileProcessing(File file, OcrRequest ocrRequest, OcrDataRequest dataRequest, String clientId, boolean preprocessed) {
        ocrProcessorService.processingExtractionText(file, ocrRequest, dataRequest, clientId, preprocessed);
    }
}
