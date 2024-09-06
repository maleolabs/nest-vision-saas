package m2codes.perizinan_ocr_tool.application.service;

import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResultDto extractTextFromImage(String imageUrl);

}