package m2codes.perizinan_ocr_tool.application.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class OcrProcessorService1 {

    private final DataEntriService dataEntriService;

    private final TextExtractionService textExtractionService;

    private final ImageUploadService imageUploadService;

    private final OcrResultService ocrResultService;

    private final ExtractedTextService extractedTextService;

    public OcrProcessorService1(
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
    public WebResponse<?> processOcr(ImageUploadRequest request) {
        ImageUpload imageUpload = imageUploadService.save(request);

        OcrResultDto ocrResultDto = textExtractionService.extractTextFromImage(request.getImageUrl());

//        if (ocrResultDto.isSuccess()) {
//             OcrResult ocrResult = ocrResultService.save(ocrResultDto, imageUpload);
//
//             log.info("SAVED OCR RESULT IN OCR PROCESSING SERVICE : {}", ocrResult);
//
//             List<ExtractedTextDto> extractedTextDto = textExtractionService.extractKeyValueFromText(ocrResultDto.getExtractedText());
//
//             extractedTextToDataEntri(extractedTextDto, request.getJenisPerizinanId(), ocrResult);
//        }

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

    public WebResponse<ExtractedTextResponse> findByTextKey(String textKey, Long izinId) {
        WebResponse<ExtractedTextResponse> webResponse = new WebResponse<>();
        ExtractedTextResponse response = new ExtractedTextResponse();

        try {
            extractedTextService.findByTextKey(textKey, izinId).ifPresentOrElse(extractedText -> {
                response.setTextKey(extractedText.getTextKey());
                response.setTextValue(extractedText.getTextValue());

                webResponse.setData(response);
                webResponse.setSuccess(true);
            }, () -> {
                webResponse.setSuccess(false);
                webResponse.setErrorMessage("extracted text with key: " + textKey + " and izinId: " + izinId + " not found.");
            });
        } catch (RuntimeException exception) {
            webResponse.setSuccess(false);
            webResponse.setErrorMessage(exception.getMessage());
        }

        return webResponse;
    }

    public WebResponse<List<ExtractedTextResponse>> findByIzinId(Long izinId) {
        WebResponse<List<ExtractedTextResponse>> webResponse = new WebResponse<>();

        List<ExtractedTextResponse> responses = new ArrayList<>();

        imageUploadService.findByIzinId(izinId).ifPresentOrElse(imageUpload -> {
            if (imageUpload.getOcrResults() != null) {
                List<ExtractedText> extractedTexts = imageUpload.getOcrResults().getExtractedText();
                for (ExtractedText extractedText : extractedTexts) {
                    ExtractedTextResponse extractedTextResponse = ExtractedTextResponse.builder()
                            .textKey(extractedText.getTextKey())
                            .textValue(extractedText.getTextValue())
                            .build();
                    responses.add(extractedTextResponse);
                }
                webResponse.setSuccess(true);
                webResponse.setData(responses);
            }
        }, () -> {
            webResponse.setSuccess(false);
            webResponse.setErrorMessage("extracted text with izinId: " + izinId + " not found.");
        });

        return webResponse;
    }

}