package m2codes.ocr_tool.application.service;

import jakarta.transaction.Transactional;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class TextProcessorService {

    protected final OcrRequestService ocrRequestService;
    protected  final OcrResultService ocrResultService;
    protected  final ExtractedTextService extractedTextService;

    public TextProcessorService(
            OcrRequestService ocrRequestService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService
    ) {
        this.ocrRequestService = ocrRequestService;
        this.ocrResultService = ocrResultService;
        this.extractedTextService = extractedTextService;
    }

    @Transactional
    public final ApiResponse<?> processOcrRequestWithImageUrl(OcrDataRequest request, String clientId) {
        var status = isPoolAvailable() ? RequestStatus.PROCESSING : RequestStatus.WAITING;
        OcrRequest ocrRequest = saveOcrRequest(request, status, clientId);
        processingExtractionText(request, ocrRequest, clientId);
        return buildResponse(
                OcrResponse.builder()
                        .requestId(ocrRequest.getId().toString())
                        .status(ocrRequest.getStatus())
                        .build(),
                true,
                null
        );
    }

    public final ApiResponse<?> processOcrRequestWithUploadedImage(OcrDataRequest request, String clientId) {
        var status = isPoolAvailable() ? RequestStatus.PROCESSING : RequestStatus.WAITING;

        String uploadedFileName;
        File uploadedFile;
        try {
            uploadedFileName = uploadFile(request.getImage());
            uploadedFile = retrieveFile(uploadedFileName);
        } catch (Exception e) {
            log.error("Exception error : {}. Stack trace : {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
            return buildResponse(null, false,
                    "Failed to process file upload! Please try again later.");
        }

        if (uploadedFile == null || uploadedFileName == null) {
            return buildResponse(null, false,
                    "File upload failed! Please try again later");
        }

        OcrDataRequest dataRequest = OcrDataRequest.builder().imageUrl(uploadedFileName).build();
        OcrRequest ocrRequest = saveOcrRequest(dataRequest, status, clientId);
        processingExtractionText(uploadedFile, ocrRequest, dataRequest, clientId);

        return buildResponse(
                OcrResponse.builder()
                        .requestId(ocrRequest.getId().toString())
                        .status(ocrRequest.getStatus())
                        .build(),
                true,
                null
        );
    }
    protected abstract void processingExtractionText(OcrDataRequest request, OcrRequest ocrRequest, String clientId);

    protected abstract void processingExtractionText(File file, OcrRequest ocrRequest, OcrDataRequest dataRequest, String clientid);

    protected abstract OcrRequest saveOcrRequest(OcrDataRequest request, RequestStatus status, String clientId);

    protected abstract OcrResultDto extractTextFromImage(String imageUrl);

    protected abstract OcrResultDto extractTextFromImage(File file);

    protected abstract OcrResult saveOcrResult(OcrResultDto ocrResultDto, OcrRequest ocrRequest);

    protected abstract CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText, List<String> requiredKeys);

    protected abstract CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText);

    protected abstract void saveAllExtractedText(OcrResultDto ocrResultDto, List<String> requiredKeys, OcrResult ocrResult);

    protected abstract void saveAllExtractedText(OcrResultDto ocrResultDto, OcrResult ocrResult);

    protected abstract String uploadFile(MultipartFile file) throws Exception;

    protected abstract File retrieveFile(String fileName) throws Exception;

    protected abstract boolean isPoolAvailable();

    protected abstract <T> ApiResponse<T> buildResponse(T data, boolean success, String errorMessage);

}