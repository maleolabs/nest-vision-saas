package m2codes.perizinan_ocr_tool.domain.service.impl;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.repository.ApiKeyRepository;
import m2codes.perizinan_ocr_tool.domain.service.ApiKeyService;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyServiceImpl(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public ApiKey save(String clientId, String apiKey, Long expiresAt) {
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
    public ApiKey findByApiKey(String apiKey) {
        return apiKeyRepository.findFirstByApiKey(apiKey).orElse(null);
    }

    @Override
    public ApiKey findByClientId(String clientId) {
        return apiKeyRepository.findFirstByClientId(clientId).orElse(null);
    }

    @Override
    public void deleteByClientId(String clientId) {
        apiKeyRepository.deleteByClientId(clientId);
    }

}