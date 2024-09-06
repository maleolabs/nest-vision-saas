package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ImageUploadService {

    ImageUpload save(ImageUploadRequest request);

    List<ImageUpload> findByIzinId(Long izinId);

    Optional<ImageUpload> findFirstByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId);

}