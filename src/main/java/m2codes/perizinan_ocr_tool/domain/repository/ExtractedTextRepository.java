package m2codes.perizinan_ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

}