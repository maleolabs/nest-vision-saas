package m2codes.perizinan_ocr_tool.domain.service.impl;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKey save(String clientId, String apiKey, Instant expiresAt) {
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
                .build()
        );

        return apiKeyData;
    }

    @Override
    public Optional<ApiKey> findByApiKey(String apiKey) {
        return apiKeyRepository.findFirstByApiKey(apiKey);
    }

    @Override
    public Optional<ApiKey> findByClientId(String clientId) {
        return apiKeyRepository.findFirstByClientId(clientId);
    }

    @Override
    public void deleteByClientId(String clientId) {
        apiKeyRepository.deleteByClientId(clientId);
    }

}