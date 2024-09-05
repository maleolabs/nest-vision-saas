package m2codes.perizinan_ocr_tool.infrastructure.app;

import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.repository.ImageUploadRepository;
import m2codes.perizinan_ocr_tool.domain.service.ImageUploadService;
import m2codes.perizinan_ocr_tool.web.dto.request.ImageUploadRequest;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private final ImageUploadRepository imageUploadRepository;

    public ImageUploadServiceImpl(ImageUploadRepository imageUploadRepository) {
        this.imageUploadRepository = imageUploadRepository;
    }

    @Override
    public ImageUpload save(ImageUploadRequest request) {


        ImageUpload imageUpload = ImageUpload.builder()
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