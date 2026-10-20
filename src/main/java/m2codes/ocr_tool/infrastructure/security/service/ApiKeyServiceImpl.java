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
import java.util.Optional;
import java.util.List;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;

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
    public String create(String clientId) {
        String apiKey = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        save(clientId, apiKey, expiresAt);
        return apiKey;
    }

    @Override
    public boolean verify(String apiKey) {
        return lookup(apiKey).map(k -> Boolean.TRUE.equals(k.getIsActive()) && !k.getExpiresAt().isBefore(Instant.now())).orElse(false);
    }

    @Override
    public void delete(String apiKey) {

    }

    @Override
    public String findByClientId(String clientId) {
        return apiKeyRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
            .filter(k -> Boolean.TRUE.equals(k.getIsActive()) && !k.getExpiresAt().isBefore(Instant.now()))
            .findFirst().map(this::plainToken).orElseThrow();
    }

    @Override public String rotate(String clientId) { return create(clientId); }
    @Override public void revoke(String apiKey) { lookup(apiKey).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }
    @Override public String clientId(String apiKey) { return lookup(apiKey).filter(k -> Boolean.TRUE.equals(k.getIsActive()) && !k.getExpiresAt().isBefore(Instant.now())).map(ApiKey::getClientId).orElse(null); }
    @Override public List<ApiKeyResponse> list(String clientId) { return apiKeyRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream().map(k -> new ApiKeyResponse(k.getId(), "API key", Boolean.TRUE.equals(k.getIsActive()), k.getCreatedAt(), k.getExpiresAt(), null)).toList(); }
    @Override public void revokeForClient(Long id, String clientId) { apiKeyRepository.findById(id).filter(k -> clientId.equals(k.getClientId())).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }

    private void save(String clientId, String apiKey, Instant expiresAt) {
        apiKeyRepository.save(
            ApiKey.builder()
                .clientId(clientId)
                .apiKey(null)
                .keyHash(apiKeyGenerator.hash(apiKey))
                .expiresAt(expiresAt)
                .isActive(true)
                .build()
        );

    }

    private Optional<ApiKey> lookup(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return token.startsWith("app_v2_")
            ? apiKeyRepository.findByKeyHash(apiKeyGenerator.hash(token))
            : apiKeyRepository.findFirstByApiKey(token);
    }

    private String plainToken(ApiKey key) {
        if (key.getApiKey() == null) throw new IllegalStateException("API key secret unavailable");
        return key.getApiKey();
    }

}
