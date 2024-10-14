package m2codes.perizinan_ocr_tool.application.service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.OcrRequestService;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ExtractedTextQueryService {

    protected final ExtractedTextService extractedTextService;

    protected final OcrRequestService ocrRequestService;

    public ExtractedTextQueryService(
            ExtractedTextService extractedTextService,
            OcrRequestService ocrRequestService
    ) {
        this.extractedTextService = extractedTextService;
        this.ocrRequestService = ocrRequestService;
    }

    public final WebResponse<ExtractedTextResponse> findByTextKey(String textKey, Long izinId) {
        return handleTextKeyLookup(textKey, izinId);
    }

    public final WebResponse<List<ExtractedTextResponse>> findByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId) {
        List<ExtractedTextResponse> responses = new ArrayList<>();

        return findIUByIzinIdAndSyaratIzinId(izinId, syaratIzinId)
                .map(imageUpload -> {
                    if (imageUpload.getOcrResults() != null) {
                        responses.addAll(mapExtractedTextsToResponses(imageUpload.getOcrResults().getExtractedText()));
                        return buildWebResponse(responses, true, null);
                    }
                    return buildWebResponse(responses, false, "Extracted text with izinId: " + izinId + " not found.");
                })
                .orElseGet(() -> buildWebResponse(responses, false, "Extracted text with izinId: " + izinId + " not found."));
    }

    public final WebResponse<List<ExtractedTextResponse>> findByIzinId(Long izinId) {
        List<ExtractedTextResponse> responses = new ArrayList<>();

        findIUByIzinId(izinId)
                .forEach(imageUpload -> {
                    if (imageUpload.getOcrResults() != null) {
                        responses.addAll(mapExtractedTextsToResponses(imageUpload.getOcrResults().getExtractedText()));
                    }
                });

        if (!responses.isEmpty())
            return buildWebResponse(responses, true, null);
        else
            return buildWebResponse(responses, false, "Extracted text with izinId: " + izinId + " not found.");
    }

    public final WebResponse<OcrResponse> checkStatus(Long requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return buildWebResponse(null, false, "Request with id : " + requestId + " not found!");
        }
        OcrResponse response = OcrResponse.builder()
                .requestId(requestId)
                .status(request.getStatus())
                .build();
        if (request.getOcrResults() != null) {
            response.setDuration((float) (request.getOcrResults().getDuration() / 1000));
        }
        return buildWebResponse(response, true, null);
    }

    public final OcrResponse findByRequestId(Long requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return null;
        }
        var ocrResponse = OcrResponse.builder()
                .requestId(request.getId())
                .status(request.getStatus())
                .build();
        if (request.getOcrResults() != null) {
            var ocrResult = request.getOcrResults();
            ocrResponse.setDuration((float) (ocrResult.getDuration() / 1000));
            ocrResponse.setOriginalExtractedText(ocrResult.getOriginalExtractedText());

            List<ExtractedTextResponse> extractedTexts = new ArrayList<>();
            for (ExtractedText extractedText : ocrResult.getExtractedText()) {
                var extractedTextRes = ExtractedTextResponse.builder()
                        .textKey(extractedText.getTextKey())
                        .textValue(extractedText.getTextValue())
                        .build();
                extractedTexts.add(extractedTextRes);
            }
            ocrResponse.setExtractedTexts(extractedTexts);
        }
        return ocrResponse;
    }

    protected WebResponse<ExtractedTextResponse> handleTextKeyLookup(String textKey, Long izinId) {
        try {
            return findETByTextKeyAndIzinId(textKey, izinId)
                    .map(extractedText -> {
                        ExtractedTextResponse extractedTextResponse = ExtractedTextResponse.builder()
                                .textKey(extractedText.getTextKey())
                                .textValue(extractedText.getTextValue())
                                .build();
                        return buildWebResponse(extractedTextResponse, true, null);
                    })
                    .orElseGet(() -> buildWebResponse(
                            null,
                            false,
                            "Extracted text with key: " + textKey + " and izinId: " + izinId + " not found."));
        } catch (RuntimeException e) {
            return buildWebResponse(null, false, e.getMessage());
        }
    }

    protected abstract Optional<ExtractedText> findETByTextKeyAndIzinId(String textKey, Long izinId);

    protected abstract Optional<OcrRequest> findIUByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId);

    protected abstract List<OcrRequest> findIUByIzinId(Long izinId);

    protected abstract List<ExtractedTextResponse> mapExtractedTextsToResponses(List<ExtractedText> extractedTexts);

    protected abstract <T> WebResponse<T> buildWebResponse(T data, boolean success, String errorMessage);

    protected abstract OcrRequest findRequestById(Long requestId);

}