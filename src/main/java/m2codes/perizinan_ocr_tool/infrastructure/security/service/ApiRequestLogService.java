package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import m2codes.perizinan_ocr_tool.domain.repository.ApiRequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiRequestLogService {

    private final ApiRequestLogRepository apiRequestLogRepository;

    public Page<ApiRequestLog> findAllByClientId(String clientId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        return apiRequestLogRepository.findAllByClientIdOrderByRequestTimeDesc(pageable, clientId);
    }

}