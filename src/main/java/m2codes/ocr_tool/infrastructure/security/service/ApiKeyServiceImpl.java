package m2codes.ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.model.ApiKey;
import m2codes.ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;

@RequiredArgsConstructor
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;

    @Value("${app.api-key.lifetime}")
    private int apiKeyLifetimeInDays;

    @Override
    @Transactional
    public String create(String clientId, String name) {
        String apiKey = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        save(clientId, name, apiKey, expiresAt);
        return apiKey;
    }

    @Override
    public boolean verify(String apiKey) {
        return lookup(apiKey).map(this::isUsable).orElse(false);
    }

    @Override
    public void delete(String apiKey) {
        revoke(apiKey);
    }

    /** Rotation creates another active key; old key remains valid until revoke or expiry. */
    @Override
    @Transactional
    public String rotate(String clientId) { return create(clientId, "Rotated API key"); }
    @Override public void revoke(String apiKey) { lookup(apiKey).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }
    @Override public String clientId(String apiKey) { return lookup(apiKey).filter(this::isUsable).map(ApiKey::getClientId).orElse(null); }
    @Override public List<ApiKeyResponse> list(String clientId) { return apiKeyRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream().map(k -> new ApiKeyResponse(k.getId(), k.getName(), isUsable(k), k.getCreatedAt(), k.getExpiresAt(), null)).toList(); }
    @Override public void revokeForClient(Long id, String clientId) { apiKeyRepository.findById(id).filter(k -> clientId.equals(k.getClientId())).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }

    private void save(String clientId, String name, String apiKey, Instant expiresAt) {
        apiKeyRepository.save(
                ApiKey.builder()
                    .clientId(clientId)
                    .name(name)
                    .apiKey(null)
                    .keyHash(apiKeyGenerator.hash(apiKey))
                    .expiresAt(expiresAt)
                    .isActive(true)
                    .build()
        );
    }

    private boolean isUsable(ApiKey key) {
        return Boolean.TRUE.equals(key.getIsActive())
                && key.getExpiresAt() != null
                && !key.getExpiresAt().isBefore(Instant.now());
    }

    private Optional<ApiKey> lookup(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return apiKeyRepository.findByKeyHash(apiKeyGenerator.hash(token));
    }

}
