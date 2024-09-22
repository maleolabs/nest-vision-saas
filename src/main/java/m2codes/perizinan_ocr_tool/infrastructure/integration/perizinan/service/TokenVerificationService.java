package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;

import java.util.concurrent.CompletableFuture;

public interface TokenVerificationService {

    CompletableFuture<UserResponse> getCurrentUser(String token);

}