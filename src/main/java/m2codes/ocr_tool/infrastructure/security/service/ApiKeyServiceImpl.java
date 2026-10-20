package m2codes.ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.ApiKey;
import m2codes.ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public String create(String clientId) {
        String rawKey = "app_" + randomKey();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        save(clientId, hash(rawKey), expiresAt);
        return rawKey;
    }

    @Override
    public boolean verify(String apiKey) {
        var apiKeyData = apiKeyRepository.findFirstByApiKey(hash(apiKey)).orElseThrow();
        return !apiKeyData.getExpiresAt().isBefore(Instant.now());
    }

    @Override
    public void delete(String apiKey) {

    }

    @Override
    public String findByClientId(String clientId) {
        throw new IllegalStateException("API key cannot be retrieved after creation");
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
                .name("Default API key")
                .apiKey(apiKey)
                .expiresAt(expiresAt)
                .isActive(true)
                .build()
        );

    }

    private String randomKey() {
        byte[] bytes = new byte[Math.max(16, (apiKeyLength * 3) / 4)];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, apiKeyLength);
    }

    private String hash(String value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

}
