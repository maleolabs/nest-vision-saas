package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

    Optional<OcrResult> findFirstByImageUpload(ImageUpload imageUpload);

}