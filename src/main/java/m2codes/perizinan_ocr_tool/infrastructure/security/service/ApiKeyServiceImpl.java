package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import m2codes.perizinan_ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    @Value("${app.api-key.length}")
    private int apiKeyLength;
    @Value("${app.api-key.lifetime}")
    private int apiKeyLifetimeInDays;

    @Override
    public void create(String clientId) {
        try {
            String apiKey = apiKeyGenerator.encrypt(clientId);
            Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);

            save(clientId, apiKey, expiresAt);
        } catch (Exception e) {
            log.error("failed to create API Key, got error: {}, {}", e.getMessage(), e.toString());
        }
    }

    @Override
    public boolean verify(String apiKey) {
        var apiKeyData = apiKeyRepository.findFirstByApiKey(apiKey).orElseThrow();
        return !apiKeyData.getExpiresAt().isBefore(Instant.now());
    }

    @Override
    public void delete(String apiKey) {

    }

    @Override
    public String findByClientId(String clientId) {
        return apiKeyRepository.findFirstByClientId(clientId).orElseThrow().getApiKey();
    }

    private void save(String clientId, String apiKey, Instant expiresAt) {
        var apiKeyData = apiKeyRepository.findFirstByClientId(clientId).orElse(null);
        if (apiKeyData != null) {
            apiKeyData.setApiKey(apiKey);
            apiKeyData.setExpiresAt(expiresAt);
            apiKeyRepository.save(apiKeyData);
            return;
        }

        apiKeyRepository.save(
            ApiKey.builder()
                .clientId(clientId)
                .apiKey(apiKey)
                .expiresAt(expiresAt)
                .isActive(true)
                .build()
        );

    }

}