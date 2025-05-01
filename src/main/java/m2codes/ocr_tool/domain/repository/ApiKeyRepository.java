package m2codes.ocr_tool.domain.repository;

import jakarta.transaction.Transactional;
import m2codes.ocr_tool.domain.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findFirstByClientId(String clientId);

    Optional<ApiKey> findFirstByApiKey(String apiKey);

    @Transactional
    void deleteByClientId(String clientId);

}