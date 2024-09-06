package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface ExtractedTextService {

    void save(ExtractedTextDto extractedTextDto, @NonNull OcrResult ocrResult);

    void saveAll(List<ExtractedTextDto> extractedTextDtos, @NonNull OcrResult ocrResult);

    Optional<ExtractedText> findByTextKey(String textKey, Long izinId);

}