package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ExtractedTextRepository extends JpaRepository<ExtractedText, Long> {

    Optional<ExtractedText> findFirstByTextKey(@Param("textKey") String textKey);

    Optional<ExtractedText> findFirstByOcrResult(OcrResult ocrResult);

}