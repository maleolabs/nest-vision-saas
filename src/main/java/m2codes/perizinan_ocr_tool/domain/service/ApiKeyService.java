package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;

public interface ApiKeyService {

    ApiKey save(String clientId, String apiKey, Long expiresAt);

    ApiKey findByApiKey(String apiKey);

    ApiKey findByClientId(String clientId);

    void deleteByClientId(String clientId);

}