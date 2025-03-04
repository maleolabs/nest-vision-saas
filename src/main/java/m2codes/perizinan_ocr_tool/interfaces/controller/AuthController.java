package m2codes.perizinan_ocr_tool.interfaces.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AuthService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.LoginRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@RequestMapping("auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping(path = "/register")
    public String register(Model  model) {
        model.addAttribute("registerRequest", new UserDataRequest());
        return "auth/register";
    }

    @PostMapping(path = "/register")
    public String register(
        @Valid @ModelAttribute("registerRequest") UserDataRequest request,
        BindingResult result,
        Model model
    ) {
        if (result.hasErrors()) {
            return "auth/register";
        }

        WebResponse<?> response = authService.register(request);
        if (!response.isSuccess()) {
            model.addAttribute("error", response.getErrorMessage());
            return "auth/register";
        }
        return "redirect:/account/verify";
    }

    @GetMapping(path = "/login")
    public String login(HttpSession session, Model model) {
        String errorMessage = (String) session.getAttribute("loginError");
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            session.removeAttribute("loginError");
        }

        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

}