package m2codes.perizinan_ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Slf4j
public class ExtractedTextMapper {

    public static List<ExtractedTextDto> parseLinesByColon(String[] lines) {
        List<ExtractedTextDto> mappedText = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(":", 2);

            if (parts.length < 2) continue;

            String key = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            if (value.isEmpty() || value.isBlank()) continue;

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
        ListIterator<ExtractedTextDto> parsedDataIterator = parsedData.listIterator();
        while (parsedDataIterator.hasNext()) {
            var data = parsedDataIterator.next();
            requiredKeys.stream()
                    .filter(dataEntri -> dataEntri.getNama().startsWith(data.getTextKey()))
                    .findFirst()
                    .ifPresent(matchesKey -> {
                        data.setDataEntriId(matchesKey.getId());
                        data.setTextKey(matchesKey.getNama());
                        parsedDataIterator.set(data);
                    });
        }

        return parsedData;
    }

}