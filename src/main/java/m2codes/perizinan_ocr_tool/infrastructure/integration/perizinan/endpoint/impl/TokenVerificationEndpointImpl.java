package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.impl;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.TokenVerificationEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TokenVerificationEndpointImpl implements TokenVerificationEndpoint {

    @Value("${perizinan-dpmptsp.api.current-user-path}")
    private String currentUserApiPath;

    private final WebClient client;

    public TokenVerificationEndpointImpl(@Qualifier("perizinanWebClient") WebClient client) {
        this.client = client;
    }

    @Override
    public UserResponse getCurrentUser(String token) {
        return client.get()
                .uri(currentUserApiPath)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

}