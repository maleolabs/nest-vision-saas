package m2codes.ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class TryOcrController {

    private final ExtractedTextQueryServiceImpl extractedTextQueryService;
    private final AuthService authService;
    private final ApiKeyService apiKeyService;

    @GetMapping(path = "/extract")
    public String tryOcrPage(Model model) {
        User user;
        try {
            user = authService.getCurrentUser();
        } catch (Exception e) {
            return "redirect:/auth/login";
        }
        String clientId = user.getClient().getClientId();
        model.addAttribute("clientId", clientId);

        // tryocr memakai /api/** (X-API-KEY) untuk do-ocr + check-status, jadi
        // halaman butuh sebuah key yang valid untuk client ini. Ambil key usable
        // pertama kalau ada; kalau belum ada, buat sekali. Tanpa ini template
        // merender apiKey kosong -> ApiKeyAuthFilter 401 "Token is invalid" yang
        // di-parse sebagai JSON oleh frontend.
        String apiKey = apiKeyService.list(clientId).stream()
                .filter(k -> k.secret() != null && !k.secret().isBlank())
                .map(ApiKeyResponse::secret)
                .findFirst()
                .orElseGet(() -> apiKeyService.create(clientId, "TryOCR"));
        model.addAttribute("apiKey", apiKey);

        return "tryocr/request";
    }

    @GetMapping(path = "/result/{requestId}")
    public String showTestingResultPage(
            @PathVariable("requestId") String requestId,
            Model model
    ) {
        User user;
        try {
            user = authService.getCurrentUser();
        } catch (Exception e) {
            return "redirect:/auth/login";
        }

        OcrResponse ocrResponse = extractedTextQueryService.findByRequestId(requestId);
        if (ocrResponse == null || !user.getClient().getClientId().equals(ocrResponse.getClientId())) {
            return "error/404";
        }
        model.addAttribute("ocrResult", ocrResponse);
        return "tryocr/result";
    }

}
