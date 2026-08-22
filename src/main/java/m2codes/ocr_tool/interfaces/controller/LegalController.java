package m2codes.ocr_tool.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Static legal pages. Content reflects actual application behavior
 * (local storage of uploads, request logs, API key encryption).
 */
@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy() {
        return "legal/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "legal/terms";
    }
}
