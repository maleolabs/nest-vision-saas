package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.impl;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.ClientTokenResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.TokenVerificationEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TokenVerificationEndpointImpl implements TokenVerificationEndpoint {

    @Value("${perizinan-dpmptsp.api.current-user-path}")
    private String currentUserApiPath;

    @Value("${perizinan-dpmptsp.api.validating-client-token-path}")
    private String validatingClientTokenPath;

    private final WebClient client;

    public TokenVerificationEndpointImpl(@Qualifier("perizinanWebClient") WebClient client) {
        this.client = client;
    }

    @Override
    @Cacheable(
            value = "tokenCache",
            key = "#token",
            unless = "#result == null",
            cacheManager = "cacheManager"
    )
    public UserResponse getCurrentUser(String token) {
        return client.get()
                .uri(currentUserApiPath)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    @Override
//    @Cacheable(
//            value = "clientTokenCache",
//            key = "#clientToken",
//            unless = "#result == null || #result.isValid() == false",
//            cacheManager = "cacheManager"
//    )
    public ClientTokenResponse verifyClientToken(String clientToken) {
        return client.post()
                .uri(uriBuilder -> uriBuilder
                        .path(validatingClientTokenPath)
                        .build(clientToken)
                )
                .retrieve()
                .bodyToMono(ClientTokenResponse.class)
                .block();
    }
}