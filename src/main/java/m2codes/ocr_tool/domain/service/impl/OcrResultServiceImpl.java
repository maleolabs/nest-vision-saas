package m2codes.ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.repository.OcrResultRepository;
import m2codes.ocr_tool.domain.service.OcrResultService;
import m2codes.ocr_tool.application.dto.OcrResultDto;

import java.util.UUID;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class OcrResultServiceImpl implements OcrResultService {

    private final OcrResultRepository ocrResultRepository;

    public OcrResultServiceImpl(OcrResultRepository ocrResultRepository) {
        this.ocrResultRepository = ocrResultRepository;
    }

    @Override
    public OcrResult save(OcrResultDto ocrResultDto, @NonNull OcrRequest ocrRequest) {
        UUID savedOcrResultId = ocrResultRepository.findFirstByOcrRequest(ocrRequest).map(OcrResult::getId).orElse(null);
        OcrResult ocrResult = OcrResult.builder()
                .id(savedOcrResultId)
                .ocrRequest(ocrRequest)
                .isSuccess(ocrResultDto.isSuccess())
                .errorMessage(ocrResultDto.getErrorMessage())
                .duration(ocrResultDto.getDuration())
                .originalExtractedText(ocrResultDto.getExtractedText())
                .build();

        return ocrResultRepository.save(ocrResult);
    }

}