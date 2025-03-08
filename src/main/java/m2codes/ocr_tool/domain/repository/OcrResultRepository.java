package m2codes.ocr_tool.domain.repository;

import m2codes.ocr_tool.domain.model.OcrRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.ocr_tool.domain.model.OcrResult;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrResultRepository extends JpaRepository<OcrResult, UUID> {

    Optional<OcrResult> findFirstByOcrRequest(OcrRequest ocrRequest);

}