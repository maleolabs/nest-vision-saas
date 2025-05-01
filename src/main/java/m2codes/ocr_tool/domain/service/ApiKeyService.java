package m2codes.ocr_tool.domain.service;

public interface ApiKeyService {

    void create(String clientId);

    boolean verify(String apiKey);

    String findByClientId(String clientId);

    void delete(String apiKey);

}