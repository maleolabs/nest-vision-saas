package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.ClientTokenResponse;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.UserResponse;

public interface TokenVerificationEndpoint {

    UserResponse getCurrentUser(String token);

    ClientTokenResponse verifyClientToken(String token);

}