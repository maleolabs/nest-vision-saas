package m2codes.perizinan_ocr_tool.interfaces.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("dashboard")
public class DashboardController {

    @GetMapping(path = "")
    public String index(Model model) {
        model.addAttribute("apiRequests", List.of(
                Map.of("month", "Jan", "count", 120),
                Map.of("month", "Feb", "count", 200),
                Map.of("month", "Mar", "count", 150)
        ));

        model.addAttribute("endpointUsage", List.of(
                Map.of("endpoint", "Ekstraksi", "count", 300),
                Map.of("endpoint", "Cek Proses", "count", 150),
                Map.of("endpoint", "Ambil Hasil", "count", 100)
        ));

        model.addAttribute("successRate", List.of(
                Map.of("type", "Sukses", "value", 450),
                Map.of("type", "Gagal", "value", 50)
        ));

        return "dashboard/index";
    }

    @GetMapping(path = "/docs")
    public String docs() {
        return "dashboard/docs";
    }

}