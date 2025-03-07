package m2codes.ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.ocr_tool.domain.model.OcrRequest;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrRequestRepository extends  JpaRepository<OcrRequest, Long> {
    Optional<OcrRequest> findFirstByImageUrl(String imageUrl);

}