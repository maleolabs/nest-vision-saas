package m2codes.perizinan_ocr_tool.interfaces.controller;

import m2codes.perizinan_ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.perizinan_ocr_tool.application.service.impl.OcrProcessorService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> doOcr(@RequestBody OcrDataRequest request) {
        WebResponse<?> response = ocrProcessorService.processOcrRequest(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping(
            path = "/do-ocr-with-direct-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> doOcr(@RequestParam("file")MultipartFile file) {
        WebResponse<?> response = ocrProcessorService.processOcrRequest(file);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find-text/{izinId}/{textKey}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<ExtractedTextResponse>> findByTextKey(@PathVariable(name = "textKey") String textKey, @PathVariable(name = "izinId") Long izinId) {
        WebResponse<ExtractedTextResponse> response = extractedTextQueryService.findByTextKey(textKey, izinId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find/{izinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<List<ExtractedTextResponse>>> findByIzinId(@PathVariable(name = "izinId") Long izinId) {
        WebResponse<List<ExtractedTextResponse>> response = extractedTextQueryService.findByIzinId(izinId);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find/{izinId}/{syaratIzinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<List<ExtractedTextResponse>>> findByIzinIdAndSyaratIzinId(@PathVariable(name = "izinId") Long izinId, @PathVariable(name = "syaratIzinId") Long syaratIzinId) {
        WebResponse<List<ExtractedTextResponse>> response = extractedTextQueryService.findByIzinIdAndSyaratIzinId(izinId, syaratIzinId);
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