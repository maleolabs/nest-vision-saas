package m2codes.ocr_tool.application.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.util.TaskManager;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.infrastructure.filemanager.service.FileManagerService;
import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.service.TextProcessorService;
import m2codes.ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.ocr_tool.application.util.ExtractedTextMapper;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OcrProcessorService extends TextProcessorService {

    private final TextExtractionService textExtractionService;
    private final ExtractedTextCleaner extractedTextCleaner;
    private final ExtractedTextMapper extractedTextMapper;
    private final TaskManager taskManager;
    private final FileManagerService fileManagerService;

    @PersistenceContext
    private EntityManager entityManager;

    public OcrProcessorService(
            OcrRequestService ocrRequestService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService,
            TextExtractionService textExtractionService,
            ExtractedTextCleaner extractedTextCleaner,
            ExtractedTextMapper extractedTextMapper,
            TaskManager taskManager,
            FileManagerService fileManagerService
    ) {
        super(ocrRequestService, ocrResultService, extractedTextService);
        this.textExtractionService = textExtractionService;

        this.extractedTextCleaner = extractedTextCleaner;
        this.extractedTextMapper = extractedTextMapper;

        this.taskManager = taskManager;

        this.fileManagerService = fileManagerService;
    }

    @Async
    @Transactional
    @Override
    protected void processingExtractionText(OcrDataRequest request, OcrRequest ocrRequest, String clientId) {
        ocrRequest = entityManager.merge(ocrRequest);

        OcrResultDto ocrResultDto = extractTextFromImage(request.getImageUrl());
        if (!ocrResultDto.isSuccess()) {
            saveOcrRequest(request, RequestStatus.FAILURE, clientId);
            entityManager.flush();
            return;
        }

        OcrResult ocrResult = saveOcrResult(ocrResultDto, ocrRequest);
        saveAllExtractedText(ocrResultDto, request.getRequiredKeys(), ocrResult);
        ocrRequestService.updateStatus(ocrRequest, RequestStatus.DONE);

        entityManager.flush();
    }

    @Async
    @Transactional
    @Override
    protected void processingExtractionText(File file, OcrRequest ocrRequest, OcrDataRequest dataRequest, String clientId) {
        ocrRequest = entityManager.merge(ocrRequest);

        try {
            OcrResultDto ocrResultDto = extractTextFromImage(file);
            if (!ocrResultDto.isSuccess()) {
                saveOcrRequest(dataRequest, RequestStatus.FAILURE, clientId);
                entityManager.flush();
            }
            OcrResult ocrResult = saveOcrResult(ocrResultDto, ocrRequest);
            saveAllExtractedText(ocrResultDto, ocrResult);
            ocrRequestService.updateStatus(ocrRequest, RequestStatus.DONE);
            entityManager.flush();
        } catch (Exception e) {
            log.error("Exception Error : {}", e.getMessage());
            saveOcrRequest(dataRequest, RequestStatus.FAILURE, clientId);
            entityManager.flush();
        }
    }

    @Override
    protected OcrRequest saveOcrRequest(OcrDataRequest request, RequestStatus status, String clientId) {
        return ocrRequestService.save(request, status, clientId);
    }

    @Override
    protected OcrResultDto extractTextFromImage(String imageUrl) {
        return textExtractionService.extractTextFromImage(imageUrl);
    }

    @Override
    protected OcrResultDto extractTextFromImage(File file) {
        return textExtractionService.extractTextFromImage(file);
    }

    @Override
    protected OcrResult saveOcrResult(OcrResultDto ocrResultDto, OcrRequest ocrRequest) {
        return ocrResultService.save(ocrResultDto, ocrRequest);
    }

    @Async
    @Override
    protected CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText, List<String> requiredKeys) {
        String[] lines = extractedText.split("\\r?\\n");
        String[] cleanLines = extractedTextCleaner.linesCleaner(lines);

        List<ExtractedTextDto> extractedTextDtos = new ArrayList<>(extractedTextMapper.parseLinesByColon(cleanLines));
        extractedTextDtos.addAll(extractedTextMapper.detectAndAddMissingKeyValue(cleanLines, requiredKeys));

        List<ExtractedTextDto> filteredData = extractedTextMapper.filterParsedDataByRequiredKeys(extractedTextDtos, requiredKeys);
        return CompletableFuture.completedFuture(filteredData);
    }

    @Async
    @Override
    protected CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText) {
        String[] lines = extractedText.split("\\r?\\n");
        String[] cleanLines = extractedTextCleaner.linesCleaner(lines);

        List<ExtractedTextDto> extractedTextDtos = new ArrayList<>(extractedTextMapper.parseLinesByColon(cleanLines));
        return CompletableFuture.completedFuture(extractedTextDtos);
    }

    @Override
    protected void saveAllExtractedText(OcrResultDto ocrResultDto, List<String> requiredKeys, OcrResult ocrResult) {
        CompletableFuture<List<ExtractedTextDto>> future = processExtractedText(ocrResultDto.getExtractedText(), requiredKeys);
        future.thenAccept(extractedTextDtos -> extractedTextService.saveAll(extractedTextDtos, ocrResult));
    }

    @Override
    protected void saveAllExtractedText(OcrResultDto ocrResultDto, OcrResult ocrResult) {
        CompletableFuture<List<ExtractedTextDto>> future = processExtractedText(ocrResultDto.getExtractedText());
        future.thenAccept(extractedTextDtos -> extractedTextService.saveAll(extractedTextDtos, ocrResult));
    }

    @Override
    protected <T> ApiResponse<T> buildResponse(T data, boolean success, String errorMessage) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setData(data);
        apiResponse.setSuccess(success);
        apiResponse.setErrorMessage(errorMessage);
        return apiResponse;
    }

    @Override
    protected boolean isPoolAvailable() {
        return taskManager.isPoolAvailable();
    }

    @Override
    protected String uploadFile(MultipartFile file) throws Exception {
        return fileManagerService.uploadFile(file);
    }

    @Override
    protected File retrieveFile(String fileName) throws Exception {
        InputStream fileInputStream = fileManagerService.retrieveFile(fileName);
        return fileManagerService.createTempFileFromInputStream(fileInputStream, fileName);
    }

}