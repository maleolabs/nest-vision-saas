package m2codes.perizinan_ocr_tool.interfaces.controller;

import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/ocr")
public class OcrController {

    private final OcrProcessorService1 ocrProcessorService1;

    public OcrController(OcrProcessorService1 ocrProcessorService1) {
        this.ocrProcessorService1 = ocrProcessorService1;
    }

    @PostMapping(
            path = "/do-ocr",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<?>> doOcr(@RequestBody ImageUploadRequest request) {
        WebResponse<?> response = ocrProcessorService1.processOcr(request);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find-by-text-key/{textKey}/{izinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<ExtractedTextResponse>> findByTextKeyAnd(@PathVariable(name = "textKey") String textKey, @PathVariable(name = "izinId") Long izinId) {
        WebResponse<ExtractedTextResponse> response = ocrProcessorService1.findByTextKey(textKey, izinId);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @GetMapping(
            path = "/find-by-izin-id/{izinId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<WebResponse<List<ExtractedTextResponse>>> findByIzinId(@PathVariable(name = "izinId") Long izinId) {
        WebResponse<List<ExtractedTextResponse>> response = ocrProcessorService1.findByIzinId(izinId);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

}