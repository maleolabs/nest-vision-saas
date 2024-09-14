package m2codes.perizinan_ocr_tool.application.service;

import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author marij_mokoginta
 */
public interface TextExtractionService {

    CompletableFuture<OcrResultDto> extractTextFromImage(String imageUrl);

}