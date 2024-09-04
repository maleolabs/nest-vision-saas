package m2codes.perizinan_ocr_tool.domain.service;

import org.springframework.lang.NonNull;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrResultService {

    OcrResult save(OcrResultDto ocrResultDto, @NonNull ImageUpload imageUpload);

}