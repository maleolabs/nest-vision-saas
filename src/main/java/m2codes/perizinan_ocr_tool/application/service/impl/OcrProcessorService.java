package m2codes.perizinan_ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.application.service.TextProcessorService;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static m2codes.perizinan_ocr_tool.application.util.ExtractedTextCleaner.linesCleaner;
import static m2codes.perizinan_ocr_tool.application.util.ExtractedTextMapper.*;

@Slf4j
@Service
public class OcrProcessorService extends TextProcessorService {

    private final TextExtractionService textExtractionService;
    private final DataEntriService dataEntriService;

    public OcrProcessorService(
            ImageUploadService imageUploadService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService,
            TextExtractionService textExtractionService,
            DataEntriService dataEntriService
    ) {
        super(imageUploadService, ocrResultService, extractedTextService);
        this.textExtractionService = textExtractionService;
        this.dataEntriService = dataEntriService;
    }

    @Override
    protected ImageUpload saveImageUpload(ImageUploadRequest request) {
        return imageUploadService.save(request);
    }

    @Override
    protected OcrResultDto extractTextFromImage(String imageUrl) {
        return textExtractionService.extractTextFromImage(imageUrl);
    }

    @Override
    protected OcrResult saveOcrResult(OcrResultDto ocrResultDto, ImageUpload imageUpload) {
        return ocrResultService.save(ocrResultDto, imageUpload);
    }

    @Override
    protected List<DataEntriDto> getDataEntri(Long jenisPerizinanId) {
        return Optional.ofNullable(dataEntriService.getByJenisPerizinanId(jenisPerizinanId).block()).orElseThrow();
    }

    @Override
    protected List<ExtractedTextDto> processExtractedText(String extractedText, List<DataEntriDto> dataEntri) {
        String[] lines = extractedText.split("\\r?\\n");
        String[] cleanLines = linesCleaner(lines);

        Arrays.stream(cleanLines)
                .forEach(System.out::println);

        List<ExtractedTextDto> extractedTextDtos = new ArrayList<>();
        extractedTextDtos.addAll(parseLinesByColon(cleanLines));
        extractedTextDtos.addAll(detectAndAddMissingKeyValue(cleanLines, dataEntri));

        log.info("PROCESSED EXTRACTED TEXT BEFORE FILTERED : {}", extractedTextDtos.size());
        extractedTextDtos.forEach(extractedTextDto -> log.info("TEXT PROCESSED : {} -> {}", extractedTextDto.getTextKey(), extractedTextDto.getTextValue()));

        return filterParsedDataByRequiredKeys(extractedTextDtos, dataEntri);
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