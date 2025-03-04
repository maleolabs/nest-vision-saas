package m2codes.perizinan_ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.AccountStatus;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AccountVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("account")
@RequiredArgsConstructor
public class AccountVerificationController {

    private final UserService userService;
    private final AccountVerificationService accountVerificationService;

    @GetMapping(path = "/verify")
    public String verificationPage(Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user != null && !user.getStatus().equals(AccountStatus.PENDING)) {
            return "redirect:/dashboard";
        }
        return "auth/verification";
    }

}