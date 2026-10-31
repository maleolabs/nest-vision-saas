package m2codes.ocr_tool.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null) {
            try {
                if (!apiKeyService.verify(apiKey)) {
                    reject(response, "Token is invalid");
                    return;
                }
                request.setAttribute("clientId", apiKeyService.clientId(apiKey));
            } catch (Exception e) {
                reject(response, "Token Verification Failed");
                return;
            }
        } else {
            reject(response, "Missing or Invalid Authorization Header");
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Penolakan selalu JSON supaya klien yang mem-parsing respons dengan res.json()
     * tidak mendapat SyntaxError "Unexpected token 'T'". ponytail: klien lama yang
     * membaca body teks mentah akan pecah; upgrade path = baca field errorMessage.
     */
    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"errorMessage\":\"" + message + "\"}");
    }
}
