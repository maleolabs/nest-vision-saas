package m2codes.ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.repository.AccountTypeRepository;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import m2codes.ocr_tool.interfaces.dto.request.AccountDataRequest;
import m2codes.ocr_tool.interfaces.dto.request.LoginRequest;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("auth")
public class AuthController {

    private final AuthService authService;
    private final AccountTypeRepository accountTypeRepository;

    @GetMapping(path = "/register")
    public String register(Model  model) {
        try {
            User user = authService.getCurrentUser();
            if (user != null) {
                return "redirect:/dashboard";
            }
        } catch (Exception e) {
            log.error("failed to get current user on register, got error: {}", e.getMessage());
        }

        model.addAttribute("registerRequest", new AccountDataRequest());
        model.addAttribute("accountTypes", accountTypeRepository.findAll());
        return "auth/register";
    }

    @PostMapping(path = "/register")
    public String register(
        @Valid @ModelAttribute("registerRequest") AccountDataRequest request,
        BindingResult result,
        Model model
    ) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        ApiResponse<?> response = authService.register(request);
        if (!response.isSuccess()) {
            model.addAttribute("error", response.getErrorMessage());
            return "auth/register";
        }
        return "redirect:/auth/login";
    }

    @GetMapping(path = "/login")
    public String login(HttpSession session, Model model) {
        try {
            User user = authService.getCurrentUser();
            if (user != null) {
                return "redirect:/dashboard";
            }
        } catch (Exception e) {
            log.error("failed to get current user on login, got error: {}", e.getMessage());
        }

        String errorMessage = (String) session.getAttribute("loginError");
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            session.removeAttribute("loginError");
        }

        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

}