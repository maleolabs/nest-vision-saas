package m2codes.perizinan_ocr_tool.application.service;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.OcrRequestService;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

import java.util.List;
import java.util.NoSuchElementException;
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

    public final WebResponse<?> processingExtractionText(OcrDataRequest request) {
        OcrResultDto ocrResultDto = extractTextFromImage(request.getImageUrl());

        if (!ocrResultDto.isSuccess()) {
            return buildWebResponse(ocrResultDto);
        }

        OcrRequest ocrRequest = saveOcrRequest(request);
        OcrResult ocrResult = saveOcrResult(ocrResultDto, ocrRequest);

        List<DataEntriDto> dataEntri;
        try {
            dataEntri = getDataEntri(request.getJenisPerizinanId());
        } catch (NoSuchElementException e) {
            ocrResultDto.setSuccess(false);
            ocrResultDto.setErrorMessage(e.getMessage());
            return buildWebResponse(ocrResultDto);
        }

        CompletableFuture<List<ExtractedTextDto>> future = processExtractedText(ocrResultDto.getExtractedText(), dataEntri);
        future.thenAccept(extractedTextDtos -> saveAllExtractedText(extractedTextDtos, ocrResult));

        return buildWebResponse(ocrResultDto);
    }

    protected abstract OcrRequest saveOcrRequest(OcrDataRequest request);

    protected abstract OcrResultDto extractTextFromImage(String imageUrl);

    protected abstract OcrResult saveOcrResult(OcrResultDto ocrResultDto, OcrRequest ocrRequest);

    protected abstract List<DataEntriDto> getDataEntri(Long jenisPerizinanId);

    protected abstract CompletableFuture<List<ExtractedTextDto>> processExtractedText(String extractedText, List<DataEntriDto> dataEntri);

    protected abstract void saveAllExtractedText(List<ExtractedTextDto> extractedTextDtos, OcrResult ocrResult);

    protected abstract WebResponse<?> buildWebResponse(OcrResultDto ocrResultDto);

}