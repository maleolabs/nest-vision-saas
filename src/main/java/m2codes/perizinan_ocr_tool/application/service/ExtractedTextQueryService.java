package m2codes.perizinan_ocr_tool.application.service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class ExtractedTextQueryService {

    protected final ExtractedTextService extractedTextService;

    protected final ImageUploadService imageUploadService;

    public ExtractedTextQueryService(
            ExtractedTextService extractedTextService,
            ImageUploadService imageUploadService
    ) {
        this.extractedTextService = extractedTextService;
        this.imageUploadService = imageUploadService;
    }

    public final WebResponse<ExtractedTextResponse> findByTextkey(String textKey, Long izinId) {
        return handleTextKeyLookup(textKey, izinId);
    }

    public final WebResponse<List<ExtractedTextResponse>> findByIzinId(Long izinId) {
        List<ExtractedTextResponse> responses = new ArrayList<>();

        return findFirstByIzinId(izinId)
                .map(imageUpload -> {
                    if (imageUpload.getOcrResults() != null) {
                        responses.addAll(mapExtractedTextsToResponses(imageUpload.getOcrResults().getExtractedText()));
                        return buildWebResponse(responses, true, null);
                    }
                    return buildWebResponse(responses, false, "Extracted text with izinId: " + izinId + " not found.");
                })
                .orElseGet(() -> buildWebResponse(responses, false, "Extracted text with izinId: " + izinId + " not found."));
    }

    protected WebResponse<ExtractedTextResponse> handleTextKeyLookup(String textKey, Long izinId) {
        try {
            return findFirstByTextKey(textKey, izinId)
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

    protected abstract Optional<ExtractedText> findFirstByTextKey(String textKey, Long izinId);

    protected abstract Optional<ImageUpload> findFirstByIzinId(Long izinId);

    protected abstract List<ExtractedTextResponse> mapExtractedTextsToResponses(List<ExtractedText> extractedTexts);

    protected abstract <T> WebResponse<T> buildWebResponse(T data, boolean success, String errorMessage);

}