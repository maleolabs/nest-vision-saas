package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;

import java.time.Instant;

public interface ApiKeyService {

    ApiKey save(String clientId, String apiKey, Instant expiresAt);

    ApiKey findByApiKey(String apiKey);

    ApiKey findByClientId(String clientId);

    void deleteByClientId(String clientId);

}