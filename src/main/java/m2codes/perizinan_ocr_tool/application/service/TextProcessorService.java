package m2codes.perizinan_ocr_tool.application.service;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
public abstract class TextProcessorService {

    protected final ImageUploadService imageUploadService;
    protected  final OcrResultService ocrResultService;
    protected  final ExtractedTextService extractedTextService;

    public TextProcessorService(
            ImageUploadService imageUploadService,
            OcrResultService ocrResultService,
            ExtractedTextService extractedTextService
    ) {
        this.imageUploadService = imageUploadService;
        this.ocrResultService = ocrResultService;
        this.extractedTextService = extractedTextService;
    }

    public final WebResponse<?> processingExtractionText(ImageUploadRequest request) {
        ImageUpload imageUpload = saveImageUpload(request);
        OcrResultDto ocrResultDto = extractTextFromImage(request.getImageUrl());

        if (ocrResultDto.isSuccess()) {
            OcrResult ocrResult = saveOcrResult(ocrResultDto, imageUpload);

            List<DataEntriDto> dataEntri;
            try {
                dataEntri = getDataEntri(request.getJenisPerizinanId());
            } catch (NoSuchElementException e) {
                ocrResultDto.setSuccess(false);
                ocrResultDto.setErrorMessage(e.getMessage());
                return buildWebResponse(ocrResultDto);
            }
            List<ExtractedTextDto> extractedTextDtos = processExtractedText(ocrResultDto.getExtractedText(), dataEntri);

            log.info("PROCESSED EXTRACTED TEXT AFTER FILTERED : {}", extractedTextDtos.size());

            saveAllExtractedText(extractedTextDtos, ocrResult);
        }

        return buildWebResponse(ocrResultDto);
    }

    protected abstract ImageUpload saveImageUpload(ImageUploadRequest request);

    protected abstract OcrResultDto extractTextFromImage(String imageUrl);

    protected abstract OcrResult saveOcrResult(OcrResultDto ocrResultDto, ImageUpload imageUpload);

    protected abstract List<DataEntriDto> getDataEntri(Long jenisPerizinanId);

    protected abstract List<ExtractedTextDto> processExtractedText(String extractedText, List<DataEntriDto> dataEntri);

    protected abstract void saveAllExtractedText(List<ExtractedTextDto> extractedTextDtos, OcrResult ocrResult);

    protected abstract WebResponse<?> buildWebResponse(OcrResultDto ocrResultDto);

}