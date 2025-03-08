package m2codes.ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.ocr_tool.application.service.impl.OcrProcessorService;
import m2codes.ocr_tool.infrastructure.security.service.RequestLimitService;
import m2codes.ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/ocr")
@Slf4j
@RequiredArgsConstructor
public class OcrController {

    private final OcrProcessorService ocrProcessorService;
    private final ExtractedTextQueryServiceImpl extractedTextQueryService;
    private final ApiKeyGenerator apiKeyGenerator;
    private final RequestLimitService requestLimitService;

    @PostMapping(
            path = "/do-ocr",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<?>> doOcr(
            @ModelAttribute @Valid OcrDataRequest request,
            BindingResult result,
            HttpServletRequest servletRequest
            ) {
        String apiKey = servletRequest.getHeader("X-API-KEY");
        String clientId;

        try {
            clientId = apiKeyGenerator.decrypt(apiKey);
            if (!requestLimitService.isRequestAllowed(clientId)) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "Request limit exceeded for today.",
                                HttpStatus.BAD_REQUEST
                        )
                );
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error(
                            "Failed to process request",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
            );
        }

        if (result.hasErrors()) {
            List<String> errors = result.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();

            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(errors)
                    .errorMessage("Validation Error")
                    .build());
        }

        if (!(request.isUsingFile() || request.isUsingUrl())) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .success(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(null)
                    .errorMessage("Must send image or imageUrl")
                    .build());
        }

        ApiResponse<?> response = request.isUsingUrl()
                                ? ocrProcessorService.processOcrRequestWithImageUrl(request, clientId)
                                : ocrProcessorService.processOcrRequestWithUploadedImage(request, clientId);

        if (response.isSuccess()) {
            requestLimitService.incrementUsage(clientId);

            return ResponseEntity.ok().body(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/get-result/{requestId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<?>> getByRequestId(
            @PathVariable(name = "requestId") String requestId,
            HttpServletRequest servletRequest
    ) {
        String apiKey = servletRequest.getHeader("X-API-KEY");
        String clientId;

        try {
            clientId = apiKeyGenerator.decrypt(apiKey);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error(
                            "Failed to get request result",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
            );
        }

        var response = extractedTextQueryService.getByRequestId(requestId);

        var invalidClientId = checkClientId(clientId, response.getData().getClientId());
        if (invalidClientId != null) {
            return invalidClientId;
        }

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/check-status/{requestId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<?>> checkStatus(
            @PathVariable(name = "requestId") String requestId,
            HttpServletRequest servletRequest
    ) {
        String apiKey = servletRequest.getHeader("X-API-KEY");
        String clientId;

        try {
            clientId = apiKeyGenerator.decrypt(apiKey);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    ApiResponse.error(
                            "Failed to get request result",
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
            );
        }
        ApiResponse<OcrResponse> response = extractedTextQueryService.checkStatus(requestId);

        var invalidClientId = checkClientId(clientId, response.getData().getClientId());
        if (invalidClientId != null) {
            return invalidClientId;
        }

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<ApiResponse<?>> checkClientId(String requestClientId, String actualClientId) {
        if (requestClientId.equals(actualClientId)) {
            return null;
        }

        return ResponseEntity.notFound().build();
    }

}