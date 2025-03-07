package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.application.dto.OcrResultDto;

import java.io.File;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResultDto extractTextFromImage(String imageUrl);

    OcrResultDto extractTextFromImage(File file);

}