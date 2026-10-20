package m2codes.ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.ocr_tool.domain.model.OcrRequest;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrRequestRepository extends  JpaRepository<OcrRequest, UUID> {
    // XR-07: dedupe selalu scoped per client
    Optional<OcrRequest> findFirstByImageUrlAndClientId(String imageUrl, String clientId);

    boolean existsByImageUrlAndClientId(String imageUrl, String clientId);

}