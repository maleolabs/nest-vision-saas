package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.service.ExtractedTextQueryService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.stereotype.Service;

@Service
public class ExtractedTextQueryServiceImpl extends ExtractedTextQueryService {

    public ExtractedTextQueryServiceImpl(
            ExtractedTextService extractedTextService,
            OcrRequestService ocrRequestService
    ) {
        super(extractedTextService, ocrRequestService);
    }

    @Override
    protected OcrRequest findRequestById(Long requestId) {
        return ocrRequestService.find(requestId).orElse(null);
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