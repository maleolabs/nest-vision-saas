package m2codes.ocr_tool.application.service;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
public abstract class TextProcessorService {

    protected final OcrRequestService ocrRequestService;
    protected  final OcrResultService ocrResultService;
    protected  final ExtractedTextService extractedTextService;
    protected final OcrProcessingAsyncExecutor ocrProcessingAsyncExecutor;

    public TextProcessorService(
            OcrRequestService ocrRequestService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService,
            OcrProcessingAsyncExecutor ocrProcessingAsyncExecutor
    ) {
        this.ocrRequestService = ocrRequestService;
        this.ocrResultService = ocrResultService;
        this.extractedTextService = extractedTextService;
        this.ocrProcessingAsyncExecutor = ocrProcessingAsyncExecutor;
    }

    /**
     * VIS-F01: @Transactional sengaja dihapus. Method ini hanya menulis satu OcrRequest
     * (repository.save dengan tx sendiri) dan harus selesai COMMIT sebelum task async
     * dijalankan; menggabungkannya dalam tx HTTP membuat worker async bisa merge entitas
     * yang belum commit (race do-ocr fast-return). Pemanggilan @Async kini via
     * OcrProcessingAsyncExecutor agar proxy @Async tidak di-bypass self-invocation.
     */
    public final ApiResponse<?> processOcrRequestWithImageUrl(OcrDataRequest request, String clientId) {
        var status = isPoolAvailable() ? RequestStatus.PROCESSING : RequestStatus.WAITING;

        boolean preprocessed = usePreprocessedImage(request.getImageUrl(), clientId);

        OcrRequest ocrRequest = saveOcrRequest(request, status, clientId);
        dispatchUrlProcessing(request, ocrRequest, clientId, preprocessed);
        return buildResponse(
                OcrResponse.builder()
                        .requestId(ocrRequest.getId().toString())
                        .status(ocrRequest.getStatus())
                        .build(),
                true,
                null
        );
    }

    private void dispatchUrlProcessing(OcrDataRequest request, OcrRequest ocrRequest, String clientId, boolean preprocessed) {
        try {
            ocrProcessingAsyncExecutor.dispatchUrlProcessing(request, ocrRequest, clientId, preprocessed);
        } catch (RejectedExecutionException e) {
            // pool + queue penuh (AppConfig RejectedExecutionHandler): tanpa worker yang
            // mengerjakan task, OcrRequest bisa stuck non-terminal — tandai FAILURE lalu
            // biarkan ErrorController menjawab 503 (bukan fake-success).
            log.warn("OCR executor saturated; marking request {} FAILURE", ocrRequest.getId());
            markFailureQuietly(ocrRequest);
            throw e;
        }
    }


    /**
     * VIS-F01: @Transactional dihapus dengan alasan sama dengan processOcrRequestWithImageUrl —
     * save harus commit sebelum task async start.
     */
    public final ApiResponse<?> processOcrRequestWithUploadedImage(OcrDataRequest request, String clientId) {
        var status = isPoolAvailable() ? RequestStatus.PROCESSING : RequestStatus.WAITING;

        String uploadedFileName;
        File uploadedFile;
        try {
            uploadedFileName = uploadFile(request.getImage());
            uploadedFile = retrieveFile(uploadedFileName);
        } catch (Exception e) {
            return buildResponse(null, false,
                    "Failed to process file upload! Please try again later.");
        }

        if (uploadedFile == null || uploadedFileName == null) {
            return buildResponse(null, false,
                    "File upload failed! Please try again later");
        }

        boolean preprocessed = usePreprocessedImage(uploadedFileName, clientId);

        OcrDataRequest dataRequest = OcrDataRequest.builder().imageUrl(uploadedFileName).build();
        OcrRequest ocrRequest = saveOcrRequest(dataRequest, status, clientId);
        dispatchFileProcessing(uploadedFile, ocrRequest, dataRequest, clientId, preprocessed);

        return buildResponse(
                OcrResponse.builder()
                        .requestId(ocrRequest.getId().toString())
                        .status(ocrRequest.getStatus())
                        .build(),
                true,
                null
        );
    }

    private void dispatchFileProcessing(File file, OcrRequest ocrRequest, OcrDataRequest dataRequest, String clientId, boolean preprocessed) {
        try {
            ocrProcessingAsyncExecutor.dispatchFileProcessing(file, ocrRequest, dataRequest, clientId, preprocessed);
        } catch (RejectedExecutionException e) {
            log.warn("OCR executor saturated; marking request {} FAILURE", ocrRequest.getId());
            markFailureQuietly(ocrRequest);
            throw e;
        }
    }

    /**
     * Tanpa worker yang mengerjakan task (pool + queue penuh), OcrRequest bisa stuck di
     * PROCESSING/WAITING selamanya — tandai FAILURE quietly agar tidak jadi orphan.
     */
    private void markFailureQuietly(OcrRequest ocrRequest) {
        try {
            ocrRequestService.updateStatus(ocrRequest, RequestStatus.FAILURE);
        } catch (Exception e) {
            log.error("Failed to mark OcrRequest {} as FAILURE", ocrRequest.getId(), e);
        }
    }

    public abstract void processingExtractionText(OcrDataRequest request, OcrRequest ocrRequest, String clientId, boolean preprocessed);

    /**
     * @Async proxy-safe: method ini WAJIB public dan dipanggil dari bean lain
     * (OcrProcessingAsyncExecutor), bukan lewat self-invocation, agar dieksekusi
     * pada ocrTaskExecutor — bukan thread HTTP.
     */
    public abstract void processingExtractionText(File file, OcrRequest ocrRequest, OcrDataRequest dataRequest, String clientid, boolean preprocessed);

    protected abstract OcrRequest saveOcrRequest(OcrDataRequest request, RequestStatus status, String clientId);

    protected abstract OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed);

    protected abstract OcrResultDto extractTextFromImage(File file, boolean preprocessed);

    protected abstract OcrResult saveOcrResult(OcrResultDto ocrResultDto, OcrRequest ocrRequest);

    protected abstract CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText, List<String> requiredKeys);

    protected abstract CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText);

    protected abstract void saveAllExtractedText(OcrResultDto ocrResultDto, List<String> requiredKeys, OcrResult ocrResult);

    protected abstract void saveAllExtractedText(OcrResultDto ocrResultDto, OcrResult ocrResult);

    protected abstract String uploadFile(MultipartFile file) throws Exception;

    protected abstract File retrieveFile(String fileName) throws Exception;

    protected abstract boolean usePreprocessedImage(String imageUrl, String clientId);

    protected abstract boolean isPoolAvailable();

    protected abstract <T> ApiResponse<T> buildResponse(T data, boolean success, String errorMessage);

}