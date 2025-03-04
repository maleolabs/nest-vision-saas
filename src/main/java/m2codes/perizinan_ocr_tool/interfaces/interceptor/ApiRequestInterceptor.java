package m2codes.perizinan_ocr_tool.interfaces.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import m2codes.perizinan_ocr_tool.domain.repository.ApiRequestLogRepository;
import m2codes.perizinan_ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiRequestInterceptor implements HandlerInterceptor {

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ApiRequestLog log = ApiRequestLog.builder()
                .endpoint(request.getRequestURI())
                .httpMethod(request.getMethod())
                .ipAddress(getIpAddress(request))
                .responseStatus(response.getStatus())
                .clientId(getClientIdFromRequest(request))
                .requestTime(Instant.now())
                .build();

        apiRequestLogRepository.save(log);
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getClientIdFromRequest(HttpServletRequest request) {
        String apiKeyHeader = request.getHeader("X-API-KEY");
        try {
            if (apiKeyHeader != null && apiKeyHeader.startsWith("app_")) {
                return apiKeyGenerator.decrypt(apiKeyHeader);
            }
        } catch (Exception e) {
            log.error("failed to get client id from request, got error: {}", e.getMessage());
        }
        return null;
    }
}