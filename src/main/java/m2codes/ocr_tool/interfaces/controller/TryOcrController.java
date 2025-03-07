package m2codes.ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ocr")
@RequiredArgsConstructor
public class TryOcrController {

    private final ExtractedTextQueryServiceImpl extractedTextQueryService;
    private final AuthService authService;
    private final ApiKeyService apiKeyService;

    @GetMapping(path = "/extract")
    public String tryOcrPage(Model model) {
        try {
            User user = authService.getCurrentUser();
            String apiKey = apiKeyService.findByClientId(user.getClient().getClientId());
            model.addAttribute("apiKey", apiKey);
        } catch (Exception e) {
            return "redirect:/auth/login";
        }
        return "tryocr/request";
    }

    @GetMapping(path = "/result/{requestId}")
    public String showTestingResultPage(@PathVariable("requestId") Long requestId, Model model) {
        OcrResponse ocrResponse = extractedTextQueryService.findByRequestId(requestId);
        if (ocrResponse == null) {
            return "error/404";
        }
        model.addAttribute("ocrResult", ocrResponse);
        return "tryocr/result";
    }

}
