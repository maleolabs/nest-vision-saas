package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.application.dto.OcrResultDto;

import java.io.File;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed);

    OcrResultDto extractTextFromImage(File file, boolean preprocessed);

}