package m2codes.perizinan_ocr_tool.interfaces.controller;

import m2codes.perizinan_ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.OcrResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/testing")
public class TestingController {

    private final ExtractedTextQueryServiceImpl extractedTextQueryService;

    public TestingController(ExtractedTextQueryServiceImpl extractedTextQueryService) {
        this.extractedTextQueryService = extractedTextQueryService;
    }

    @GetMapping(path = "/request")
    public String showTestingPage() {
        return "ocr-testing";
    }

    @GetMapping(path = "/result/{requestId}")
    public String showTestingResultPage(@PathVariable("requestId") Long requestId, Model model) {
        OcrResponse ocrResponse = extractedTextQueryService.findByRequestId(requestId);
        if (ocrResponse == null) {
            return "not-found";
        }
        model.addAttribute("ocrResult", ocrResponse);
        return "ocr-testing-result";
    }

}
