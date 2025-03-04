package m2codes.perizinan_ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.infrastructure.security.service.AccountVerificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("account")
@RequiredArgsConstructor
public class AccountVerificationController {

    private final AccountVerificationService accountVerificationService;

    @GetMapping(path = "/verify")
    public String verificationPage() {
        return "auth/verification";
    }

}