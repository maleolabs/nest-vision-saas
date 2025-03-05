package m2codes.perizinan_ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.ApiRequestLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final ApiRequestLogService apiRequestLogService;

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
    public String docs(Model model, Principal principal, HttpSession session) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user != null) {
            try {
                String apiKey = apiKeyService.findByClientId(user.getClient().getClientId());
                model.addAttribute("apiKey", apiKey);
            } catch (Exception e) {
                model.addAttribute("canGenerateApiKey", true);
            }
        }

        String apiKeyError = (String) session.getAttribute("apiKeyError");
        if (apiKeyError != null) {
            model.addAttribute("apiKeyError", apiKeyError);
            session.removeAttribute("apiKeyError");
        }

        return "dashboard/docs";
    }

    @PostMapping(path = "/docs/create-new-api-key")
    public String createNewApiKey(HttpSession session, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null || user.getClient() == null) {
            session.setAttribute("apiKeyError",
                    "Tidak dapat membuat API Key baru, data pengguna tidak ditemukan! Silahkan coba lagi nanti");
            return "redirect:/dashboard/docs?failed";
        }
        apiKeyService.create(user.getClient().getClientId());
        return "redirect:/dashboard/docs";
    }

    @GetMapping(path = "/logs")
    public String logs(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            model.addAttribute("userError", true);
        } else {
            model.addAttribute("requestLogs", apiRequestLogService.findAllByClientId(user.getClient().getClientId()));
        }
        return "dashboard/logs";
    }

    @GetMapping(path = "/profile")
    public String profile(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("accountType", user.getClient().getAccountType());
        return "dashboard/profile";
    }

}