package m2codes.perizinan_ocr_tool.infrastructure.app;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.repository.ExtractedTextRepository;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class ExtractedTextServiceImpl implements ExtractedTextService {

    private final ExtractedTextRepository extractedTextRepository;

    public ExtractedTextServiceImpl(ExtractedTextRepository extractedTextRepository) {
        this.extractedTextRepository = extractedTextRepository;;
    }

    @Override
    public ExtractedText save(ExtractedTextDto extractedTextDto, @NonNull OcrResult ocrResult) {
        ExtractedText extractedText = ExtractedText.builder()
                                        .ocrResult(ocrResult)
                                        .textKey(extractedTextDto.getTextKey())
                                        .textValue(extractedTextDto.getTextValue())
                                        .build();

        return extractedTextRepository.save(extractedText);
    }

}