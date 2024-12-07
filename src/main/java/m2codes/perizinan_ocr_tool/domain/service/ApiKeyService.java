package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.model.Client;

import java.time.Instant;
import java.util.Optional;

public interface ApiKeyService {

    ApiKey save(String clientId, String apiKey, Instant expiresAt);

    Optional<ApiKey> findByApiKey(String apiKey);

    Optional<ApiKey> findByClientId(String clientId);

    void deleteByClientId(String clientId);

}