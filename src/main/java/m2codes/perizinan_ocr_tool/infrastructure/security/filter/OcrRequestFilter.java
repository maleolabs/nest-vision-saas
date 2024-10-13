package m2codes.perizinan_ocr_tool.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.TokenVerificationService;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class OcrRequestFilter extends OncePerRequestFilter {

    private final TokenVerificationService tokenVerificationService;

    public OcrRequestFilter(
            TokenVerificationService tokenVerificationService
    ) {
        this.tokenVerificationService = tokenVerificationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authToken = request.getHeader("Authorization");
        String clientToken = request.getHeader("X-API-KEY");
        if (authToken != null || clientToken != null) {
            try {
                boolean isValid;

                if (authToken != null) {
                    UserResponse userResponse = tokenVerificationService.getCurrentUser(authToken).join();
                    isValid = userResponse != null;
                } else {
                    isValid = tokenVerificationService.isClientTokenValid(clientToken).join();
                }

                if (!isValid) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid Token");
                    return;
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token Verification Failed " + e.getMessage());
                return;
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or Invalid Authorization Header");
            return;
        }
        filterChain.doFilter(request, response);
    }
}