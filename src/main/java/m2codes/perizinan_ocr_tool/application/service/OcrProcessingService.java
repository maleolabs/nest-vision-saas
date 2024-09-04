package m2codes.perizinan_ocr_tool.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.client.service.DataEntriService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.web.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.web.dto.response.WebResponse;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class OcrProcessingService {

    private final DataEntriService dataEntriService;

    private final TextExtractionService textExtractionService;

    private final ImageUploadService imageUploadService;

    public OcrProcessingService(
        DataEntriService dataEntriService,
        TextExtractionService textExtractionService,
        ImageUploadService imageUploadService
    ) {
        this.dataEntriService = dataEntriService;
        this.textExtractionService = textExtractionService;
        this.imageUploadService = imageUploadService;
    }

    public WebResponse processOcr(ImageUploadRequest request) {
        OcrResultDto ocrResultDto = textExtractionService.extractTextFromImage(request.getImageUrl());

        if (ocrResultDto.isSuccess()) {
            List<ExtractedTextDto> extractedTextDto = textExtractionService.extractKeyValueFromText(ocrResultDto.getExtractedText());

            extractedTextToDataEntri(extractedTextDto, request.getJenisPerizinanId());
        }

        return WebResponse.builder()
            .success(ocrResultDto.isSuccess())
            .errorMessage(ocrResultDto.getErrorMessage())
            .build();
    }

    protected void extractedTextToDataEntri(List<ExtractedTextDto> extractedText, Long jenisPerizinanId) {
        dataEntriService.getByJenisPerizinanId(jenisPerizinanId).subscribe(dataEntri -> {
            
        });
    }

}