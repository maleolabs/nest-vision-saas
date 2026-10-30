package m2codes.ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.application.service.IdentityDocumentCheckService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/identity-document-checks")
@RequiredArgsConstructor
public class IdentityDocumentCheckController {
    private final IdentityDocumentCheckService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> submit(@ModelAttribute @Valid OcrDataRequest request, HttpServletRequest http) {
        if (!request.isUsingFile()) return ResponseEntity.badRequest().body(ApiResponse.error("image is required", HttpStatus.BAD_REQUEST));
        var response = service.submit(request.getImage(), (String) http.getAttribute("clientId"), request.getRequiredKeys());
        return ResponseEntity.accepted().body(ApiResponse.success(response, HttpStatus.ACCEPTED));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<?>> get(@PathVariable String requestId, HttpServletRequest http) {
        var response = service.get(requestId, (String) http.getAttribute("clientId"));
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("request not found", HttpStatus.NOT_FOUND));
        }
        return ResponseEntity.ok(ApiResponse.success(response, HttpStatus.OK));
    }
}
