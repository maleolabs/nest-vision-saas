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
    Optional<OcrRequest> findFirstByImageUrl(String imageUrl);

    boolean existsByImageUrlAndClientId(String imageUrl, String clientId);

}