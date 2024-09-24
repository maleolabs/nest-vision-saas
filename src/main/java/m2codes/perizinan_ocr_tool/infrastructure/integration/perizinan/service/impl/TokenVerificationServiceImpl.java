package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.impl;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.ClientTokenResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.TokenVerificationEndpoint;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.TokenVerificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class TokenVerificationServiceImpl implements TokenVerificationService {

    private final TokenVerificationEndpoint tokenVerificationEndpoint;

    public TokenVerificationServiceImpl(
            TokenVerificationEndpoint tokenVerificationEndpoint
    ) {
        this.tokenVerificationEndpoint = tokenVerificationEndpoint;
    }

    @Async
    @Override
    public CompletableFuture<UserResponse> getCurrentUser(String token) {
        UserResponse response = Optional.ofNullable(tokenVerificationEndpoint.getCurrentUser(token))
                .orElseThrow();
        return CompletableFuture.completedFuture(response);
    }

    @Async
    @Override
    public CompletableFuture<Boolean> isClientTokenValid(String token) {
        ClientTokenResponse response = Optional.ofNullable(tokenVerificationEndpoint.verifyClientToken(token)).orElseThrow();
        return CompletableFuture.completedFuture(response.getIsValid());
    }
}