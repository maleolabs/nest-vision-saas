package m2codes.perizinan_ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Component
public class ExtractedTextMapper {

    private final CustomTextExtraction customTextExtraction;

    public ExtractedTextMapper(CustomTextExtraction customTextExtraction) {
        this.customTextExtraction = customTextExtraction;
    }

    public List<ExtractedTextDto> parseLinesByColon(String[] lines) {
        return Arrays.stream(lines)
                .filter(line -> splitLine(line).length == 2)
                .map(line -> {
                    String[] parts = splitLine(line);
                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();
                    return ExtractedTextDto.builder()
                            .textKey(key)
                            .textValue(value)
                            .build();
                }).filter(textDto -> !textDto.getTextValue().isEmpty() && !textDto.getTextValue().isBlank())
                .toList();
    }

    private String[] splitLine(String line) {
        return line.split(":", 2);
    }

    public List<ExtractedTextDto> detectAndAddMissingKeyValue(String[] lines, List<String> requiredKeys) {
        return requiredKeys.stream()
                .filter(requiredKey -> getCustomText(requiredKey, lines) != null)
                .map(requiredKeyFound -> ExtractedTextDto.builder()
                        .textKey(requiredKeyFound)
                        .textValue(getCustomText(requiredKeyFound, lines))
                        .build())
                .toList();
    }

    private String getCustomText(String category, String[] lines) {
        return customTextExtraction.findTextByCategory(category, lines);
    }

    public List<ExtractedTextDto> filterParsedDataByRequiredKeys(List<ExtractedTextDto> parsedData, List<String> requiredKeys) {
        ListIterator<ExtractedTextDto> parsedDataIterator = parsedData.listIterator();
        while (parsedDataIterator.hasNext()) {
            var data = parsedDataIterator.next();
            requiredKeys.stream()
                    .filter(key -> key.equals(data.getTextKey()))
                    .findFirst()
                    .ifPresent(matchesKey -> {
                        data.setTextKey(matchesKey);
                        parsedDataIterator.set(data);
                    });
        }

        return parsedData;
    }

}