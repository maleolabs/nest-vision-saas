package m2codes.perizinan_ocr_tool.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("dashboard")
public class DashboardController {

    @GetMapping(path = "")
    public String index() {
        return "dashboard/index";
    }

    @GetMapping(path = "/docs")
    public String docs() {
        return "dashboard/docs";
    }

}