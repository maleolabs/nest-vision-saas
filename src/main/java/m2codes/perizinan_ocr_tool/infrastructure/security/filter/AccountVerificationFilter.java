package m2codes.perizinan_ocr_tool.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import m2codes.perizinan_ocr_tool.domain.model.AccountStatus;
import m2codes.perizinan_ocr_tool.domain.model.User;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccountVerificationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null && user.getStatus().equals(AccountStatus.PENDING)) {
                response.sendRedirect("/verify-account");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}