package m2codes.perizinan_ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;

/**
 *
 * @author marij_mokoginta
 */
public interface ImageUploadRepository extends  JpaRepository<ImageUpload, Long> {

}