package m2codes.perizinan_ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.repository.ImageUploadRepository;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private final ImageUploadRepository imageUploadRepository;

    public ImageUploadServiceImpl(ImageUploadRepository imageUploadRepository) {
        this.imageUploadRepository = imageUploadRepository;
    }

    @Override
    public ImageUpload save(ImageUploadRequest request) {
        log.info("IMAGE UPLOAD REQUEST IN IMAGE UPLOAD SERVICE : {}", request);

        Long savedImageUploadId = imageUploadRepository.findFirstByIzinId(request.getIzinId()).map(ImageUpload::getId).orElse(null);

        ImageUpload imageUpload = ImageUpload.builder()
                .id(savedImageUploadId)
                .izinId(request.getIzinId())
                .jenisPerizinanId(request.getJenisPerizinanId())
                .syaratIzinId(request.getSyaratIzinId())
                .imageUrl(request.getImageUrl())
                .uploadedAt(System.currentTimeMillis())
                .build();

        return imageUploadRepository.save(imageUpload);
    }

    @Override
    public Optional<ImageUpload> findByIzinId(Long izinId) {
        return imageUploadRepository.findFirstByIzinId(izinId);
    }

}