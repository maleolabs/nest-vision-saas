package m2codes.perizinan_ocr_tool.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/testing")
public class TestingController {

    @GetMapping(path = "/")
    public String showTestingPage() {
        return "ocr-testing";
    }

}
