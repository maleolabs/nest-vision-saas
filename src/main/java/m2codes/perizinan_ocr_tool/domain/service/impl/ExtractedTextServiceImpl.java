package m2codes.perizinan_ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.ExtractedText;
import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.repository.ExtractedTextRepository;
import m2codes.perizinan_ocr_tool.domain.service.ExtractedTextService;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class ExtractedTextServiceImpl implements ExtractedTextService {

    private final ExtractedTextRepository extractedTextRepository;

    public ExtractedTextServiceImpl(ExtractedTextRepository extractedTextRepository) {
        this.extractedTextRepository = extractedTextRepository;
    }

    @Override
    public void save(ExtractedTextDto extractedTextDto, @NonNull OcrResult ocrResult) {
        Long savedETId = extractedTextRepository.findFirstByOcrResultAndDataEntriId(ocrResult, extractedTextDto.getDataEntriId()).map(ExtractedText::getId).orElse(null);

        ExtractedText extractedText = ExtractedText.builder()
                .id(savedETId)
                .ocrResult(ocrResult)
                .textKey(extractedTextDto.getTextKey())
                .textValue(extractedTextDto.getTextValue())
                .dataEntriId(extractedTextDto.getDataEntriId())
                .build();

        extractedTextRepository.save(extractedText);
    }

    @Override
    public void saveAll(List<ExtractedTextDto> extractedTextDtos, @NonNull OcrResult ocrResult) {
        List<ExtractedText> extractedTexts = new ArrayList<>();

        extractedTextDtos.forEach(extractedTextDto -> {
            Long savedETId = extractedTextRepository.findFirstByOcrResultAndDataEntriId(ocrResult, extractedTextDto.getDataEntriId()).map(ExtractedText::getId).orElse(null);
            extractedTexts.add(ExtractedText.builder()
                    .id(savedETId)
                    .textKey(extractedTextDto.getTextKey())
                    .textValue(extractedTextDto.getTextValue())
                    .ocrResult(ocrResult)
                    .dataEntriId(extractedTextDto.getDataEntriId())
                    .build()
            );
        });

        extractedTextRepository.saveAll(extractedTexts);
    }

    @Override
    public Optional<ExtractedText> findByTextKey(String textKey, Long izinId) {
        return extractedTextRepository.findFirstByTextKeyAndIzinId(textKey, izinId);
    }

}