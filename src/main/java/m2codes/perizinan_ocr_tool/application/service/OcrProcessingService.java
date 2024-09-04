package m2codes.perizinan_ocr_tool.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.client.service.DataEntriService;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.domain.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.web.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.web.dto.response.WebResponse;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class OcrProcessingService {

    private final DataEntriService dataEntriService;

    private final TextExtractionService textExtractionService;

    private final ImageUploadService imageUploadService;

    private final OcrResultService ocrResultService;

    private final ExtractedTextService extractedTextService;

    public OcrProcessingService(
        DataEntriService dataEntriService,
        TextExtractionService textExtractionService,
        ImageUploadService imageUploadService,
        OcrResultService ocrResultService,
        ExtractedTextService extractedTextService
    ) {
        this.dataEntriService = dataEntriService;
        this.textExtractionService = textExtractionService;
        this.imageUploadService = imageUploadService;
        this.ocrResultService = ocrResultService;
        this.extractedTextService = extractedTextService;
    }

    @Transactional
    public WebResponse processOcr(ImageUploadRequest request) {
        ImageUpload imageUpload = imageUploadService.save(request);

        OcrResultDto ocrResultDto = textExtractionService.extractTextFromImage(request.getImageUrl());

         if (ocrResultDto.isSuccess()) {
             OcrResult ocrResult = ocrResultService.save(ocrResultDto, imageUpload);

             List<ExtractedTextDto> extractedTextDto = textExtractionService.extractKeyValueFromText(ocrResultDto.getExtractedText());

             extractedTextToDataEntri(extractedTextDto, request.getJenisPerizinanId(), ocrResult);
         }

        return WebResponse.builder()
            .success(ocrResultDto.isSuccess())
            .errorMessage(ocrResultDto.getErrorMessage())
            .build();
    }

    protected void extractedTextToDataEntri(List<ExtractedTextDto> extractedTextDtos, Long jenisPerizinanId, OcrResult ocrResult) {
        Optional.ofNullable(dataEntriService.getByJenisPerizinanId(jenisPerizinanId).block())
            .ifPresent(dataList -> dataList.forEach(dataEntri -> {
                for (ExtractedTextDto extractedTextDto : extractedTextDtos) {
                    if (dataEntri.getNama().startsWith(extractedTextDto.getTextKey())) {
                        log.info("extracted text and data entri match : {} -> {}", dataEntri.getNama(), extractedTextDto.getTextKey());

                        extractedTextDto.setTextKey(dataEntri.getNama());
                        extractedTextService.save(extractedTextDto, ocrResult);
                    }
                }
            }));
    }

}