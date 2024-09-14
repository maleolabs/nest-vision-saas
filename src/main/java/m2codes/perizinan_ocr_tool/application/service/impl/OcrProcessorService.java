package m2codes.perizinan_ocr_tool.application.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.RequestStatus;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.application.service.TextProcessorService;
import m2codes.perizinan_ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.perizinan_ocr_tool.application.util.ExtractedTextMapper;
import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.OcrRequestService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OcrProcessorService extends TextProcessorService {

    private final TextExtractionService textExtractionService;
    private final DataEntriService dataEntriService;

    private final ExtractedTextCleaner extractedTextCleaner;
    private final ExtractedTextMapper extractedTextMapper;

    private final EntityManager entityManager;

    public OcrProcessorService(
            OcrRequestService ocrRequestService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService,
            TextExtractionService textExtractionService,
            DataEntriService dataEntriService,
            ExtractedTextCleaner extractedTextCleaner,
            ExtractedTextMapper extractedTextMapper,
            EntityManager entityManager
    ) {
        super(ocrRequestService, ocrResultService, extractedTextService);
        this.textExtractionService = textExtractionService;
        this.dataEntriService = dataEntriService;

        this.extractedTextCleaner = extractedTextCleaner;
        this.extractedTextMapper = extractedTextMapper;

        this.entityManager = entityManager;
    }

    @Async
    @Transactional
    @Override
    protected void processingExtractionText(OcrDataRequest request, OcrRequest ocrRequest) {
        OcrResultDto ocrResultDto = extractTextFromImage(request.getImageUrl());

        if (!ocrResultDto.isSuccess()) return;

        OcrResult ocrResult = saveOcrResult(ocrResultDto, entityManager.merge(ocrRequest));

        List<DataEntriDto> dataEntri = List.of();
        try {
            dataEntri = getDataEntri(request.getJenisPerizinanId());
        } catch (NoSuchElementException e) {
            log.error(e.getMessage());
        }
        saveAllExtractedText(ocrResultDto, dataEntri, ocrResult);
    }

    @Override
    protected OcrRequest saveOcrRequest(OcrDataRequest request, RequestStatus status) {
        return ocrRequestService.save(request, status);
    }

    @Override
    protected OcrResultDto extractTextFromImage(String imageUrl) {
        return textExtractionService.extractTextFromImage(imageUrl)
                .thenApply(ocrResultDto -> ocrResultDto)
                .join();
    }

    @Override
    protected OcrResult saveOcrResult(OcrResultDto ocrResultDto, OcrRequest ocrRequest) {
        return ocrResultService.save(ocrResultDto, ocrRequest);
    }

    @Override
    protected List<DataEntriDto> getDataEntri(Long jenisPerizinanId) {
        try {
            return dataEntriService.getByJenisPerizinanId(jenisPerizinanId)
                    .thenApply(result -> result.stream().toList())
                    .join();
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ArrayList<>();
        }
    }

    @Async
    @Override
    protected CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText, List<DataEntriDto> dataEntri) {
        String[] lines = extractedText.split("\\r?\\n");
        String[] cleanLines = extractedTextCleaner.linesCleaner(lines);

        List<ExtractedTextDto> extractedTextDtos = new ArrayList<>(extractedTextMapper.parseLinesByColon(cleanLines));
        extractedTextDtos.addAll(extractedTextMapper.detectAndAddMissingKeyValue(cleanLines, dataEntri));

        List<ExtractedTextDto> filteredData = extractedTextMapper.filterParsedDataByRequiredKeys(extractedTextDtos, dataEntri);
        return CompletableFuture.completedFuture(filteredData);
    }

    @Override
    protected void saveAllExtractedText(OcrResultDto ocrResultDto, List<DataEntriDto> dataEntri, OcrResult ocrResult) {
        CompletableFuture<List<ExtractedTextDto>> future = processExtractedText(ocrResultDto.getExtractedText(), dataEntri);
        future.thenAccept(extractedTextDtos -> extractedTextService.saveAll(extractedTextDtos, ocrResult));
    }

    @Override
    protected WebResponse<?> buildWebResponse(boolean success, String errorMessage, OcrResponse response) {
        return WebResponse.builder()
                .success(success)
                .errorMessage(errorMessage)
                .data(response)
                .build();
    }

}