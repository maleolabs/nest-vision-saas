package m2codes.ocr_tool.infrastructure.security.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.model.VerificationCode;
import m2codes.ocr_tool.domain.repository.VerificationCodeRepository;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.domain.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final EmailSenderService emailSenderService;

    @Async
    public void sendVerificationCode(User user, String email) {
        String generatedCode = getCode();

        verificationCodeRepository.save(VerificationCode.builder()
                        .user(user)
                        .code(generatedCode)
                        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                        .build());

        String mailSubject = generatedCode + " is your confirmation code";

        emailSenderService.sendEmail(email, mailSubject, generatedCode);
    }

    @Transactional
    public boolean verifyAccount(String verificationCode) {
        VerificationCode codeData = verificationCodeRepository.findByCode(verificationCode).orElse(null);
        if (codeData == null) {
            return false;
        }

        if (codeData.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }

        try {
            userService.activateUser(codeData.getUser().getUsername());
        } catch (UsernameNotFoundException e) {
            log.error("failed to verify user account, user not found");
            return false;
        }

        apiKeyService.create(codeData.getUser().getClient().getClientId());

        verificationCodeRepository.delete(codeData);

        return true;
    }

    private String getCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

}