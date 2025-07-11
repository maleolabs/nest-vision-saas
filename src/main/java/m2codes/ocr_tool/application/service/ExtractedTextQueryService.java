package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.domain.model.ExtractedText;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse2;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;

import java.util.*;

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

    public final ApiResponse<OcrResponse> checkStatus(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return builResponse(null, false, "Request with id : " + requestId + " not found!");
        }
        OcrResponse response = OcrResponse.builder()
                .requestId(requestId)
                .clientId(request.getClientId())
                .status(request.getStatus())
                .build();
        if (request.getOcrResults() != null) {
            response.setDuration((float) (request.getOcrResults().getDuration() / 1000));
        }
        return builResponse(response, true, null);
    }

    public final ApiResponse<OcrResponse2> getByRequestId(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return builResponse(null, false, "Request ID not found");
        }
        var ocrResponse = OcrResponse2.builder()
                .requestId(request.getId().toString())
                .status(request.getStatus())
                .clientId(request.getClientId())
                .build();

        if (request.getOcrResults() != null) {
            var ocrResult = request.getOcrResults();
            ocrResponse.setDuration((float) (ocrResult.getDuration() / 1000));
            Map<String, String> extractedTexts = new HashMap<>();
            for (ExtractedText extractedText : ocrResult.getExtractedText()) {
                extractedTexts.put(extractedText.getTextKey(), extractedText.getTextValue());
            }
            ocrResponse.setExtractedTexts(extractedTexts);
        }
        return builResponse(ocrResponse, true, null);
    }

    public final OcrResponse findByRequestId(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return null;
        }
        var ocrResponse = OcrResponse.builder()
                .requestId(request.getId().toString())
                .status(request.getStatus())
                .clientId(request.getClientId())
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

    protected abstract <T> ApiResponse<T> builResponse(T data, boolean success, String errorMessage);

    protected abstract OcrRequest findRequestById(String requestId);

}