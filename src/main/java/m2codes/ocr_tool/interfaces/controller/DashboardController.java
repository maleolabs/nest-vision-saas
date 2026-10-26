package m2codes.ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.ApiRequestLog;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.domain.service.UserService;
import m2codes.ocr_tool.infrastructure.security.service.ApiRequestLogService;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import m2codes.ocr_tool.interfaces.dto.request.ChangePasswordRequest;
import m2codes.ocr_tool.interfaces.dto.request.CreateApiKeyRequest;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
        User user;
        try { user = authService.getCurrentUser(); } catch (RuntimeException e) { return "redirect:/auth/login"; }
        Client client = user.getClient();

        var requestCountPerPeriod = apiRequestLogService.getRequestCountByClientIdAndEndpoint(client.getClientId(), "do-ocr");
        var requestCountByEndpoint = apiRequestLogService.getEndpointUsageByClientId(client.getClientId());
        var requestSuccessRate = apiRequestLogService.getSuccessRateByClientId(client.getClientId());

        model.addAttribute("apiRequests", requestCountPerPeriod);
        model.addAttribute("endpointUsage", requestCountByEndpoint);
        model.addAttribute("successRate", requestSuccessRate);

        return "dashboard/index";
    }

    @GetMapping(path = "/docs")
    public String docs(Model model, HttpSession session, HttpServletRequest request) {
        String apiKeyError = (String) session.getAttribute("apiKeyError");
        if (apiKeyError != null) {
            model.addAttribute("apiKeyError", apiKeyError);
            session.removeAttribute("apiKeyError");
        }
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        model.addAttribute("baseUrl", baseUrl);

        return "dashboard/docs";
    }

    @GetMapping("/api/keys") @ResponseBody
    public ResponseEntity<?> listApiKeys() { return noStore(apiKeyService.list(authService.getCurrentUser().getClient().getClientId())); }
    @PostMapping("/api/keys") @ResponseBody
    public ResponseEntity<?> createApiKey(@Valid @RequestBody CreateApiKeyRequest request) {
         return noStore(apiKeyService.createResponse(authService.getCurrentUser().getClient().getClientId(), request.name()));
    }
    @PostMapping("/api/keys/rotate") @ResponseBody
     public ResponseEntity<?> rotateApiKey() { return noStore(apiKeyService.rotateResponse(authService.getCurrentUser().getClient().getClientId())); }
    @PostMapping("/api/keys/{id}/rotate") @ResponseBody
    public ResponseEntity<?> rotateApiKey(@PathVariable Long id) {
        return noStore(apiKeyService.rotateResponse(id, authService.getCurrentUser().getClient().getClientId()));
    }
    @PostMapping("/api/keys/{id}/revoke") @ResponseBody
    public ResponseEntity<Void> revokeApiKey(@PathVariable Long id) { apiKeyService.revokeForClient(id, authService.getCurrentUser().getClient().getClientId()); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build(); }
    @DeleteMapping("/api/keys/{id}") @ResponseBody
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) { apiKeyService.deleteForClient(id, authService.getCurrentUser().getClient().getClientId()); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build(); }
    @DeleteMapping("/api/keys/{id}") @ResponseBody
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) { apiKeyService.deleteForClient(id, authService.getCurrentUser().getClient().getClientId()); return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build(); }
    private <T> ResponseEntity<T> noStore(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }

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
