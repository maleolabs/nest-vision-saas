package m2codes.perizinan_ocr_tool.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/testing")
public class TestingController {

    @GetMapping(path = "/request")
    public String showTestingPage() {
        return "ocr-testing";
    }

    @GetMapping(path = "/result/{requestId}")
    public String showTestingResultPage(@PathVariable("requestId") Long requestId) {
        return "ocr-testing-result";
    }

}
