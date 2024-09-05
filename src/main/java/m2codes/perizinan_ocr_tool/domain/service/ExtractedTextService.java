package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ExtractedTextService {

    ExtractedText save(ExtractedTextDto extractedTextDto, @NonNull OcrResult ocrResult);

    Optional<ExtractedText> findByTextKey(String textKey, Long izinId);

}