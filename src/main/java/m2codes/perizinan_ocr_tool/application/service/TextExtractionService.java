package m2codes.perizinan_ocr_tool.application.service;

import java.util.List;

import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResultDto extractTextFromImage(String imageUrl);

    List<ExtractedTextDto> extractKeyValueFromText(String text);

}