package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ApiKey;
import m2codes.perizinan_ocr_tool.domain.model.Client;

import java.time.Instant;
import java.util.Optional;

public interface ApiKeyService {

    String create(String clientId);

    boolean verify(String apiKey);

    void delete(String apiKey);

}