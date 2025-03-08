package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.service.ExtractedTextQueryService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
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
    protected OcrRequest findRequestById(String requestId) {
        return ocrRequestService.find(requestId).orElse(null);
    }

    @Override
    protected <T> ApiResponse<T> builResponse(T data, boolean success, String errorMessage) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setData(data);
        apiResponse.setSuccess(success);
        apiResponse.setErrorMessage(errorMessage);
        return apiResponse;
    }

}