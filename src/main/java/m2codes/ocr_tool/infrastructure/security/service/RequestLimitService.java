package m2codes.ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.RequestUsage;
import m2codes.ocr_tool.domain.repository.RequestUsageRepository;
import m2codes.ocr_tool.domain.service.ClientService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class RequestLimitService {

    private final RequestUsageRepository requestUsageRepository;
    private final ClientService clientService;

    public boolean isRequestAllowed(String clientId) {
        Client client = clientService.findByClientId(clientId).orElse(null);
        if (client == null) {
            return false;
        }

        int maxRequest = client.getAccountType().getDailyLimit();

        RequestUsage usage = requestUsageRepository.findByClientIdAndRequestDate(clientId, getTodayUtcDate())
                .orElse(RequestUsage.builder().requestCount(0).build());

        return usage.getRequestCount() < maxRequest;
    }

    public void incrementUsage(String clientId) {
        RequestUsage usage = requestUsageRepository.findByClientIdAndRequestDate(clientId, getTodayUtcDate())
                .orElse(RequestUsage.builder().clientId(clientId).requestCount(0).build());

        usage.setRequestCount(usage.getRequestCount() + 1);
        requestUsageRepository.save(usage);
    }

    private Instant getTodayUtcDate() {
        return LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

}