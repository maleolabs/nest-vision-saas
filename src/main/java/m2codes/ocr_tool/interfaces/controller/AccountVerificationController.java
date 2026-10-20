package m2codes.ocr_tool.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.AccountStatus;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.service.UserService;
import m2codes.ocr_tool.infrastructure.security.service.AccountVerificationService;
import m2codes.ocr_tool.infrastructure.security.service.AuthService;
import m2codes.ocr_tool.infrastructure.security.util.InMemoryRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("account")
@RequiredArgsConstructor
@Slf4j
public class AccountVerificationController {

    private final UserService userService;
    private final AccountVerificationService accountVerificationService;
    private final AuthService authService;
    private final InMemoryRateLimiter rateLimiter;

    // VIS-F05: batas rate in-memory, semua env-overridable via Environment (tanpa
    // menulis application.properties). Default: 5 percobaan kode/menit per account,
    // 30/menit per IP, 3 resend / 10 menit per account.
    @Value("${OCR_VERIFY_RATE_ACCOUNT_MAX:5}")
    private int verifyAccountMax;
    @Value("${OCR_VERIFY_RATE_ACCOUNT_WINDOW_MILLIS:60000}")
    private long verifyAccountWindowMillis;
    @Value("${OCR_VERIFY_RATE_IP_MAX:30}")
    private int verifyIpMax;
    @Value("${OCR_VERIFY_RATE_IP_WINDOW_MILLIS:60000}")
    private long verifyIpWindowMillis;
    @Value("${OCR_VERIFY_RESEND_MAX:3}")
    private int resendMax;
    @Value("${OCR_VERIFY_RESEND_WINDOW_MILLIS:600000}")
    private long resendWindowMillis;

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
    public String verifyAccount(@RequestParam String verificationCode,
                                HttpServletRequest servletRequest,
                                HttpServletResponse servletResponse,
                                Model model) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!user.getStatus().equals(AccountStatus.PENDING)) {
            return "redirect:/dashboard";
        }

        // VIS-F05: rate limit percobaan kode — gated SEBELUM verifyAccount agar
        // brute force 6-digit tidak menghabiskan lookup DB per attempt.
        String accountKey = "verify:acct:" + user.getUsername();
        if (!rateLimiter.allow(accountKey, verifyAccountMax, verifyAccountWindowMillis)
                || !rateLimiter.allow("verify:ip:" + servletRequest.getRemoteAddr(), verifyIpMax, verifyIpWindowMillis)) {
            // 429 + halaman verifikasi dengan pesan generik (tanpa detail IP/hitungan)
            servletResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            model.addAttribute("errorMessage", "Terlalu banyak percobaan verifikasi. Silakan coba lagi nanti.");
            return "auth/verification";
        }

        if (verificationCode.length() == 6) {
            if (accountVerificationService.verifyAccount(verificationCode)) {
                // verifikasi sukses → hitungan percobaan account dibersihkan
                rateLimiter.reset(accountKey);
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
    public String resendCode(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Model model) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return "redirect:/auth/login";
        }

        if (!user.getStatus().equals(AccountStatus.PENDING)) {
            return "redirect:/dashboard";
        }

        // VIS-F05: batas resend (kirim ulang kode) per account — service resend tidak diubah
        String resendKey = "verify:resend:" + user.getUsername();
        if (!rateLimiter.allow(resendKey, resendMax, resendWindowMillis)) {
            servletResponse.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            model.addAttribute("errorMessage", "Terlalu banyak permintaan kode. Silakan coba lagi nanti.");
            return "auth/verification";
        }

        accountVerificationService.sendVerificationCode(user, user.getClient().getEmail());

        return "auth/verification";
    }

}