package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import m2codes.perizinan_ocr_tool.domain.repository.ApiRequestLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiRequestLogService {

    private final ApiRequestLogRepository apiRequestLogRepository;

    public List<ApiRequestLog> findAllByClientId(String clientId) {
        return apiRequestLogRepository.findAllByClientIdOrderByRequestTimeDesc(clientId);
    }

}