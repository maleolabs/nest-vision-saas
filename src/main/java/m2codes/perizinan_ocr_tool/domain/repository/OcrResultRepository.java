package m2codes.perizinan_ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrResultRepository extends JpaRepository<OcrResult, Long> {

}