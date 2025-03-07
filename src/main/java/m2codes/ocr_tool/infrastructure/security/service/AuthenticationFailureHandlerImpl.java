package m2codes.ocr_tool.infrastructure.security.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class AuthenticationFailureHandlerImpl implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        log.info("Login failed: {}", exception.getMessage());

        HttpSession session = request.getSession();
        String errorMessage = "Email atau password salah!";

        if (exception instanceof LockedException) {
            errorMessage = "Akun Anda dikunci. Silakan hubungi admin.";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Akun Anda belum diaktifkan.";
        } else if (exception instanceof CredentialsExpiredException) {
            errorMessage = "Password Anda sudah kadaluarsa.";
        }

        session.setAttribute("loginError", errorMessage);

        response.sendRedirect("/auth/login");
    }

}