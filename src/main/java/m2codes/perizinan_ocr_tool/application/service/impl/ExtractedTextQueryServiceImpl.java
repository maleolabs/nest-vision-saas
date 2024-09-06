package m2codes.perizinan_ocr_tool.application.service.impl;

import m2codes.perizinan_ocr_tool.application.service.ExtractedTextQueryService;
import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ExtractedTextQueryServiceImpl extends ExtractedTextQueryService {

    public ExtractedTextQueryServiceImpl(
            ExtractedTextService extractedTextService,
            ImageUploadService imageUploadService
    ) {
        super(extractedTextService, imageUploadService);
    }

    @Override
    protected Optional<ExtractedText> findFirstByTextKey(String textKey, Long izinId) {
        return extractedTextService.findByTextKey(textKey, izinId);
    }

    @Override
    protected Optional<ImageUpload> findFirstByIzinId(Long izinId) {
        return imageUploadService.findByIzinId(izinId);
    }

    @Override
    protected List<ExtractedTextResponse> mapExtractedTextsToResponses(List<ExtractedText> extractedTexts) {
        List<ExtractedTextResponse> responses = new ArrayList<>();
        for (ExtractedText extractedText : extractedTexts) {
            responses.add(ExtractedTextResponse.builder()
                    .textKey(extractedText.getTextKey())
                    .textValue(extractedText.getTextValue())
                    .build());
        }
        return responses;
    }

    @Override
    protected <T> WebResponse<T> buildWebResponse(T data, boolean success, String errorMessage) {
        WebResponse<T> webResponse = new WebResponse<>();
        webResponse.setData(data);
        webResponse.setSuccess(success);
        webResponse.setErrorMessage(errorMessage);
        return webResponse;
    }

}