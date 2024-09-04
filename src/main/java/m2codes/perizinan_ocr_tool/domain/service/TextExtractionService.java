package m2codes.perizinan_ocr_tool.domain.service;

import java.util.List;

import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResultDto extractTextFromImage(String imageUrl);

    List<ExtractedTextDto> extractKeyValueFromText(String text);

}