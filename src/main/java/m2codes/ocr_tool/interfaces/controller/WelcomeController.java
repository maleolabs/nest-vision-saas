package m2codes.ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WelcomeController {

    private final AuthService authService;

    @GetMapping("")
    public String welcome(Model model) {
        boolean isGuest;
        try {
            User user = authService.getCurrentUser();
            isGuest = (user == null);
        } catch (Exception e) {
            log.error("failed to get current user on welcome page, got error: {}", e.getMessage());
            isGuest = true;
        }
        model.addAttribute("isGuest", isGuest);
        return "welcome";
    }

}