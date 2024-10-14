package m2codes.perizinan_ocr_tool.interfaces.controller;

import m2codes.perizinan_ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
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
        model.addAttribute("ocrResult", extractedTextQueryService.findByRequestId(requestId));
        return "ocr-testing-result";
    }

}
