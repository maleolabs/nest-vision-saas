package m2codes.ocr_tool.domain.service;
import m2codes.ocr_tool.interfaces.dto.response.ApiKeyResponse;
import java.util.List;

public interface ApiKeyService {

    String create(String clientId, String name);
    ApiKeyResponse createResponse(String clientId, String name);
    default String create(String clientId) { return create(clientId, "API key"); }

    boolean verify(String apiKey);

    void delete(String apiKey);

    String rotate(String clientId);
    ApiKeyResponse rotateResponse(String clientId);
    ApiKeyResponse rotateResponse(Long id, String clientId);

    void revoke(String apiKey);

    String clientId(String apiKey);
    List<ApiKeyResponse> list(String clientId);
    void revokeForClient(Long id, String clientId);
    ApiKeyResponse rotateForClient(Long id, String clientId);

}
