package m2codes.perizinan_ocr_tool.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrRequestRepository extends  JpaRepository<OcrRequest, Long> {

    List<OcrRequest> findByIzinId(Long izinId);

    Optional<OcrRequest> findFirstByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId);

    Optional<OcrRequest> findFirstByIzinIdAndSyaratIzinIdAndImageUrl(Long izinId, Long syaratIzinId, String imageUrl);

}