package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@RequiredArgsConstructor
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    @Value("${app.api-key.length}")
    private int apiKeyLength;
    @Value("${app.api-key.lifetime}")
    private int apiKeyLifetimeInDays;

    @Override
    public String create(String clientId) {
        String apiKey = generateRandomApiKey();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);

        var savedApiKey = save(clientId, apiKey, expiresAt);

        return savedApiKey.getApiKey();
    }

    @Override
    public boolean verify(String apiKey) {
        var apiKeyData = apiKeyRepository.findFirstByApiKey(apiKey).orElseThrow();

        return !apiKeyData.getExpiresAt().isBefore(Instant.now());
    }

    @Override
    public void delete(String apiKey) {

    }

    private ApiKey save(String clientId, String apiKey, Instant expiresAt) {
        var apiKeyData = apiKeyRepository.findFirstByClientId(clientId).orElse(null);
        if (apiKeyData != null) {
            apiKeyData.setApiKey(apiKey);
            apiKeyData.setExpiresAt(expiresAt);
            return apiKeyRepository.save(apiKeyData);
        }

        apiKeyData = apiKeyRepository.save(
            ApiKey.builder()
                .clientId(clientId)
                .apiKey(apiKey)
                .expiresAt(expiresAt)
                .isActive(true)
                .build()
        );

        return apiKeyData;
    }

    private String generateRandomApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[apiKeyLength];
        random.nextBytes(keyBytes);
        return "app_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }

}