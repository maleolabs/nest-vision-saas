package m2codes.ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.ocr_tool.application.service.impl.OcrProcessorService;
import m2codes.ocr_tool.infrastructure.security.service.RequestLimitService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/api/ocr")
@Slf4j
@RequiredArgsConstructor
public class OcrController {

    private final OcrProcessorService ocrProcessorService;
    private final ExtractedTextQueryServiceImpl extractedTextQueryService;
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
        String clientId = (String) servletRequest.getAttribute("clientId");
        try {
            if (clientId == null) throw new IllegalStateException("Missing client principal");
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
            Map<String, List<String>> errors = result.getAllErrors().stream()
                    .collect(Collectors.groupingBy(
                            error -> (error instanceof FieldError) ? ((FieldError) error).getField() : "global",
                            Collectors.mapping(DefaultMessageSourceResolvable::getDefaultMessage, Collectors.toList())
                    ));

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
        String clientId = (String) servletRequest.getAttribute("clientId");

        var response = extractedTextQueryService.getByRequestId(requestId);

        if (!response.isSuccess() || response.getData() == null) {
            // XR-03/VIS-F04: request tidak ditemukan kini 404 DENGAN ApiResponse body
            // (sebelumnya data null → NPE di checkClientId → 500 generik + leak)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

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
        String clientId = (String) servletRequest.getAttribute("clientId");
        ApiResponse<OcrResponse> response = extractedTextQueryService.checkStatus(requestId);

        if (!response.isSuccess() || response.getData() == null) {
            // XR-03/VIS-F04: request tidak ditemukan kini 404 DENGAN ApiResponse body
            // (sebelumnya data null → NPE di checkClientId → 500 generik + leak)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

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

        // XR-03/VIS-F04: 404 tanpa body diganti ApiResponse standar. Tetap 404 agar
        // eksistensi request milik client lain tidak terbocorkan ke peminta.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(
                        "Request not found for this client",
                        HttpStatus.NOT_FOUND
                )
        );
    }

}
