package m2codes.ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.model.ApiKey;
import m2codes.ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import m2codes.ocr_tool.infrastructure.security.util.ApiKeyGenerator;
import m2codes.ocr_tool.infrastructure.security.util.ApiKeyEncryption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.EntityNotFoundException;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;

@RequiredArgsConstructor
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerator apiKeyGenerator;
    private final ApiKeyEncryption apiKeyEncryption;

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
    @Transactional
    public ApiKeyResponse createResponse(String clientId, String name) {
        String secret = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        ApiKey key = save(clientId, name, secret, expiresAt);
        return response(key, secret);
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
    @Override
    @Transactional
    public ApiKeyResponse rotateResponse(String clientId) {
        String secret = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        ApiKey key = save(clientId, "Rotated API key", secret, expiresAt);
        return response(key, secret);
    }
    @Override
    @Transactional
    public ApiKeyResponse rotateResponse(Long id, String clientId) {
        ApiKey oldKey = apiKeyRepository.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new EntityNotFoundException("API key not found"));
        String secret = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        // Overlap policy: old key stays active until explicit revoke or expiry.
        ApiKey key = save(clientId, oldKey.getName(), secret, expiresAt);
        return response(key, secret);
    }
    @Override public void revoke(String apiKey) { lookup(apiKey).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }
    @Override public String clientId(String apiKey) { return lookup(apiKey).filter(this::isUsable).map(ApiKey::getClientId).orElse(null); }
    @Override @Transactional public List<ApiKeyResponse> list(String clientId) {
        // Client-scoped listing returns copyable secrets only while keys remain usable.
        // Revoked/expired keys keep metadata but never expose credentials.
        return apiKeyRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(k -> {
                    migrateLegacy(k);
                    return response(k, isUsable(k) ? secret(k) : null);
                })
                .toList();
    }
    @Override public void revokeForClient(Long id, String clientId) { apiKeyRepository.findById(id).filter(k -> clientId.equals(k.getClientId())).ifPresent(k -> { k.setIsActive(false); apiKeyRepository.save(k); }); }
    @Override @Transactional public ApiKeyResponse rotateForClient(Long id, String clientId) {
        ApiKey source = apiKeyRepository.findById(id).filter(k -> clientId.equals(k.getClientId()) && isUsable(k))
                .orElseThrow(() -> new IllegalArgumentException("API key tidak ditemukan atau tidak aktif"));
        String secret = apiKeyGenerator.generate();
        Instant expiresAt = Instant.now().plus(apiKeyLifetimeInDays, ChronoUnit.DAYS);
        ApiKey key = save(clientId, source.getName(), secret, expiresAt);
        return response(key, secret);
    }

    private ApiKey save(String clientId, String name, String apiKey, Instant expiresAt) {
        var encrypted = apiKeyEncryption.encrypt(apiKey);
        return apiKeyRepository.save(
                ApiKey.builder()
                    .clientId(clientId)
                    .name(name)
                    .apiKey(null)
                    .keyHash(apiKeyGenerator.hash(apiKey))
                    .secretNonce(encrypted.nonce())
                    .secretCiphertext(encrypted.ciphertext()).secretKeyVersion(encrypted.keyVersion())
                    .expiresAt(expiresAt)
                    .isActive(true)
                    .build()
        );
    }

    private String secret(ApiKey key) {
        if (key.getSecretNonce() != null && key.getSecretCiphertext() != null) return apiKeyEncryption.decrypt(key.getSecretNonce(), key.getSecretCiphertext(), key.getSecretKeyVersion());
        return key.getApiKey();
    }
    private ApiKeyResponse response(ApiKey k, String secret) { return new ApiKeyResponse(k.getId(), k.getName(), isUsable(k), k.getCreatedAt(), k.getExpiresAt(), secret); }
    private void migrateLegacy(ApiKey key) { if (key.getApiKey() != null) { var e = apiKeyEncryption.encrypt(key.getApiKey()); key.setKeyHash(apiKeyGenerator.hash(key.getApiKey())); key.setApiKey(null); key.setSecretNonce(e.nonce()); key.setSecretCiphertext(e.ciphertext()); key.setSecretKeyVersion(e.keyVersion()); apiKeyRepository.save(key); } }

    private boolean isUsable(ApiKey key) {
        return Boolean.TRUE.equals(key.getIsActive())
                && key.getExpiresAt() != null
                && !key.getExpiresAt().isBefore(Instant.now());
    }

    @Transactional
    private Optional<ApiKey> lookup(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Optional<ApiKey> current = apiKeyRepository.findByKeyHash(apiKeyGenerator.hash(token));
        if (current.isPresent()) return current;
        // One-time migration: legacy plaintext is accepted only for authentication, then removed.
        return apiKeyRepository.findFirstByApiKey(token).map(legacy -> {
            legacy.setKeyHash(apiKeyGenerator.hash(token));
            legacy.setApiKey(null);
            var encrypted = apiKeyEncryption.encrypt(token);
            legacy.setSecretNonce(encrypted.nonce());
            legacy.setSecretCiphertext(encrypted.ciphertext());
            legacy.setSecretKeyVersion(encrypted.keyVersion());
            return apiKeyRepository.save(legacy);
        });
    }

}
