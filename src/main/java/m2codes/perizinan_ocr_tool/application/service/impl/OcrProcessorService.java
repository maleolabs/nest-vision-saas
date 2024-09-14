package m2codes.perizinan_ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.application.service.TextProcessorService;
import m2codes.perizinan_ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.perizinan_ocr_tool.application.util.ExtractedTextMapper;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OcrProcessorService extends TextProcessorService {

    private final TextExtractionService textExtractionService;
    private final DataEntriService dataEntriService;

    private final ExtractedTextCleaner extractedTextCleaner;
    private final ExtractedTextMapper extractedTextMapper;

    public OcrProcessorService(
            ImageUploadService imageUploadService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService,
            TextExtractionService textExtractionService,
            DataEntriService dataEntriService,
            ExtractedTextCleaner extractedTextCleaner,
            ExtractedTextMapper extractedTextMapper
    ) {
        super(imageUploadService, ocrResultService, extractedTextService);
        this.textExtractionService = textExtractionService;
        this.dataEntriService = dataEntriService;

        this.extractedTextCleaner = extractedTextCleaner;
        this.extractedTextMapper = extractedTextMapper;
    }

    @Override
    protected ImageUpload saveImageUpload(ImageUploadRequest request) {
        return imageUploadService.save(request);
    }

    @Override
    protected OcrResultDto extractTextFromImage(String imageUrl) {
        return textExtractionService.extractTextFromImage(imageUrl)
                .thenApply(ocrResultDto -> ocrResultDto)
                .join();
    }

    @Override
    protected OcrResult saveOcrResult(OcrResultDto ocrResultDto, ImageUpload imageUpload) {
        return ocrResultService.save(ocrResultDto, imageUpload);
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
    protected void saveAllExtractedText(List<ExtractedTextDto> extractedTextDtos, OcrResult ocrResult) {
        extractedTextService.saveAll(extractedTextDtos, ocrResult);
    }

    @Override
    protected WebResponse<?> buildWebResponse(OcrResultDto ocrResultDto) {
        return WebResponse.builder()
                .success(ocrResultDto.isSuccess())
                .errorMessage(ocrResultDto.getErrorMessage())
                .build();
    }

}