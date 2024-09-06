package m2codes.perizinan_ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ImageUploadRepository extends  JpaRepository<ImageUpload, Long> {

    List<ImageUpload> findByIzinId(Long izinId);

    Optional<ImageUpload> findFirstByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId);

}