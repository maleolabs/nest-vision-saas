package m2codes.perizinan_ocr_tool.domain.service;

import org.springframework.lang.NonNull;

import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrResultService {

    OcrResult save(OcrResultDto ocrResultDto, @NonNull OcrRequest ocrRequest);

}