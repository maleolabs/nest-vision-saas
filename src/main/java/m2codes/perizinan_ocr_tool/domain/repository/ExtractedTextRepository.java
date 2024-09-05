package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ExtractedTextRepository extends JpaRepository<ExtractedText, Long> {

    @Query(
            "SELECT et FROM ExtractedText et " +
            "JOIN  et.ocrResult or " +
            "JOIN or.imageUpload iu " +
            "WHERE iu.izinId = :izinId " +
            "AND et.textKey = :textKey"
    )
    Optional<ExtractedText> findFirstByTextKeyAndIzinId(@Param("textKey") String textKey, @Param("izinId") Long izinId);

    List<ExtractedText> findByOcrResult(OcrResult ocrResult);

    Optional<ExtractedText> findFirstByOcrResultAndTextKey(OcrResult ocrResult, String textKey);

}