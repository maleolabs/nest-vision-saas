package m2codes.perizinan_ocr_tool.web.controller;

import m2codes.perizinan_ocr_tool.application.service.OcrProcessingService;
import m2codes.perizinan_ocr_tool.web.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.web.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.web.dto.response.WebResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/ocr")
public class OcrController {

    private final OcrProcessingService ocrProcessingService;

    public OcrController(OcrProcessingService ocrProcessingService) {
        this.ocrProcessingService = ocrProcessingService;
    }

    @PostMapping(
            path = "/do-ocr",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> doOcr(@RequestBody ImageUploadRequest request) {
        WebResponse<?> response = ocrProcessingService.processOcr(request);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find-by-text-key/{textKey}/{izinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<ExtractedTextResponse>> findByTextKeyAnd(@PathVariable(name = "textKey") String textKey, @PathVariable(name = "izinId") Long izinId) {
        WebResponse<ExtractedTextResponse> response = ocrProcessingService.findByTextKey(textKey, izinId);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find-by-izin-id/{izinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<List<ExtractedTextResponse>>> findByIzinId(@PathVariable(name = "izinId") Long izinId) {
        WebResponse<List<ExtractedTextResponse>> response = ocrProcessingService.findByIzinId(izinId);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

}