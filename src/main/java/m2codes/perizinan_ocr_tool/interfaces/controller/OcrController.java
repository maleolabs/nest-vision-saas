package m2codes.perizinan_ocr_tool.interfaces.controller;

import jakarta.validation.Valid;
import m2codes.perizinan_ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.perizinan_ocr_tool.application.service.impl.OcrProcessorService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/ocr")
public class OcrController {

    private final OcrProcessorService ocrProcessorService;

    private final ExtractedTextQueryServiceImpl extractedTextQueryService;

    public OcrController(
            OcrProcessorService ocrProcessorService,
            ExtractedTextQueryServiceImpl extractedTextQueryService
    ) {
        this.ocrProcessorService = ocrProcessorService;
        this.extractedTextQueryService = extractedTextQueryService;
    }

    @PostMapping(
            path = "/do-ocr",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> doOcr(
            @ModelAttribute @Valid OcrDataRequest request,
            BindingResult result
            ) {
        if (result.hasErrors()) {
            List<String> errors = result.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();

            return ResponseEntity.badRequest().body(WebResponse.builder()
                    .success(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(errors)
                    .errorMessage("Validation Error")
                    .build());
        }

        if (!(request.isUsingFile() || request.isUsingUrl())) {
            return ResponseEntity.badRequest().body(WebResponse.builder()
                    .success(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .data(null)
                    .errorMessage("Must send image or imageUrl")
                    .build());
        }

        WebResponse<?> response = request.isUsingUrl()
                                ? ocrProcessorService.processOcrRequestWithImageUrl(request)
                                : ocrProcessorService.processOcrRequestWithUploadedImage(request);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/get-result/{requestId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> getByRequestId(@PathVariable(name = "requestId") Long requestId) {
        var response = extractedTextQueryService.getByRequestId(requestId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/check-status/{requestId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<OcrResponse>> checkStatus(@PathVariable(name = "requestId") Long requestId) {
        WebResponse<OcrResponse> response = extractedTextQueryService.checkStatus(requestId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

}