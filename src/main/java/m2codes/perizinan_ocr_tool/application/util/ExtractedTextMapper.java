package m2codes.perizinan_ocr_tool.application.util;

import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;

import java.util.ArrayList;
import java.util.List;

public class ExtractedTextMapper {

    public static List<ExtractedTextDto> parseLinesByColon(String[] lines) {
        List<ExtractedTextDto> mappedText = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(":", 2);
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
        requiredKeys.forEach(data -> {
            parsedData.removeIf(textDto -> !data.getNama().startsWith(textDto.getTextKey()));
        });

        return parsedData;
    }

}