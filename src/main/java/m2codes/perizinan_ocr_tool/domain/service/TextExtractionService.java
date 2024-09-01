package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResult extractTextFromImage(String imageUrl);

}