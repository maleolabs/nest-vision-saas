package m2codes.perizinan_ocr_tool.infrastructure.app;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.repository.OcrResultRepository;
import m2codes.perizinan_ocr_tool.domain.service.OcrResultService;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class OcrResultServiceImpl implements OcrResultService {

    private final OcrResultRepository ocrResultRepository;

    public OcrResultServiceImpl(OcrResultRepository ocrResultRepository) {
        this.ocrResultRepository = ocrResultRepository;
    }

    @Override
    public OcrResult save(OcrResultDto ocrResultDto, @NonNull ImageUpload imageUpload) {
        OcrResult ocrResult = OcrResult.builder()
                                .imageUpload(imageUpload)
                                .isSuccess(ocrResultDto.isSuccess())
                                .errorMessage(ocrResultDto.getErrorMessage())
                                .extractedAt(ocrResultDto.getExtractedAt())
                                .build();

        return ocrResultRepository.save(ocrResult);
    }

}