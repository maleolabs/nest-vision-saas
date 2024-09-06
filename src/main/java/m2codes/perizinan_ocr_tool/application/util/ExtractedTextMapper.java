package m2codes.perizinan_ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ExtractedTextMapper {

    public static List<ExtractedTextDto> parseLinesByColon(String[] lines) {
        List<ExtractedTextDto> mappedText = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(":", 2);

            if (parts.length < 2) continue;

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();
            mappedText.add(ExtractedTextDto.builder()
                    .textKey(key)
                    .textValue(value)
                    .build());
        }

        return mappedText;
    }

    public static List<ExtractedTextDto> detectAndAddMissingKeyValue(String[] lines, List<DataEntriDto> requiredKeys) {
        return List.of();
    }

    public static List<ExtractedTextDto> filterParsedDataByRequiredKeys(List<ExtractedTextDto> parsedData, List<DataEntriDto> requiredKeys) {
        return parsedData.stream()
                .filter(
                        extractedTextDto -> requiredKeys.stream().anyMatch(requiredKey -> requiredKey.getNama().startsWith(extractedTextDto.getTextKey()))
                ).peek(extractedTextDto -> requiredKeys.stream()
                        .filter(dataEntriDto -> dataEntriDto.getNama().startsWith(extractedTextDto.getTextKey()))
                        .findFirst()
                        .ifPresent(matchingKey -> {
                            extractedTextDto.setTextKey(matchingKey.getNama());
                            extractedTextDto.setDataEntriId(matchingKey.getId());
                        })
                ).toList();
    }

}