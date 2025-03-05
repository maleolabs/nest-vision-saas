package m2codes.perizinan_ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.ApiRequestLogService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AuthService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ChangePasswordRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

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
    public String docs(Model model, HttpSession session) {
        User user = authService.getCurrentUser();
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
    public String createNewApiKey(HttpSession session) {
        User user = authService.getCurrentUser();
        if (user == null || user.getClient() == null) {
            session.setAttribute("apiKeyError",
                    "Tidak dapat membuat API Key baru, data pengguna tidak ditemukan! Silahkan coba lagi nanti");
            return "redirect:/dashboard/docs?failed";
        }
        apiKeyService.create(user.getClient().getClientId());
        return "redirect:/dashboard/docs";
    }

    @GetMapping(path = "/logs")
    public String logs(
            Model model,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        if (page < 1) {
            page = 1;
        }

        if (limit < 1) {
            limit = 1;
        }

        User user = authService.getCurrentUser();
        Page<ApiRequestLog> pageLogs = apiRequestLogService.findAllByClientId(user.getClient().getClientId(), page, limit);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageLogs.getTotalPages());
        model.addAttribute("totalItems", pageLogs.getTotalElements());
        model.addAttribute("requestLogs", pageLogs.getContent());

        return "dashboard/logs";
    }

    @GetMapping(path = "/profile")
    public String profile(Model model) {
        User user = authService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("accountType", user.getClient().getAccountType());
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "dashboard/profile";
    }

    @PostMapping(path = "/profile/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult result,
            Model model,
            HttpSession session
    ) {
        try {
            User user = authService.getCurrentUser();

            model.addAttribute("user", user);
            model.addAttribute("accountType", user.getClient().getAccountType());
            model.addAttribute("changePasswordRequest", request);

            if (result.hasErrors()) {
                return "dashboard/profile";
            }

            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                result.rejectValue("oldPassword", "error.oldPassword", "password wrong.");
                return "dashboard/profile";
            }

            userService.updatePassword(user, request.getNewPassword());
        } catch (Exception e) {
            log.error("failed to change password, got error: {}", e.getMessage());

            model.addAttribute("changePasswordError", "there is an error while changing password");
            return "dashboard/profile";
        }

        if (session != null) {
            session.invalidate();
        }

        return "redirect:/auth/login?passwordChanged";
    }

}