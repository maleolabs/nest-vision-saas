package m2codes.perizinan_ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.AccountStatus;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AccountVerificationService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("account")
@RequiredArgsConstructor
@Slf4j
public class AccountVerificationController {

    private final UserService userService;
    private final AccountVerificationService accountVerificationService;
    private final AuthService authService;

    @GetMapping(path = "/verify")
    public String verificationPage() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!user.getStatus().equals(AccountStatus.PENDING)) {
            return "redirect:/dashboard";
        }
        return "auth/verification";
    }

    @PostMapping(path = "/verify")
    public String verifyAccount(@RequestParam String verificationCode, Model model) {
        if (verificationCode.length() == 6) {
            if (accountVerificationService.verifyAccount(verificationCode)) {
                return "redirect:/dashboard";
            } else {
                model.addAttribute("errorMessage", "Kode verifikasi salah atau kode sudah kadaluarsa.");
            }
        } else {
            model.addAttribute("errorMessage", "Kode verifikasi tidak valid.");
        }
        return "auth/verification";
    }

    @GetMapping(path = "/verification/resend")
    public String resendCode(Model model) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        accountVerificationService.sendVerificationCode(user, user.getClient().getEmail());

        return "auth/verification";
    }

}