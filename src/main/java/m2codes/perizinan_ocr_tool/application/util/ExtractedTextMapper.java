package m2codes.perizinan_ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.application.dto.ExtractedTextDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    public List<ExtractedTextDto> detectAndAddMissingKeyValue(String[] lines, List<DataEntriDto> requiredKeys) {
        return requiredKeys.stream()
                .filter(requiredKey -> customTextExtraction.findTextByCategory(requiredKey.getNama(), lines) != null)
                .map(requiredKeyFound -> ExtractedTextDto.builder()
                        .textKey(requiredKeyFound.getNama())
                        .textValue(customTextExtraction.findTextByCategory(requiredKeyFound.getNama(), lines))
                        .build())
                .toList();
    }

    public List<ExtractedTextDto> filterParsedDataByRequiredKeys(List<ExtractedTextDto> parsedData, List<DataEntriDto> requiredKeys) {
        ListIterator<ExtractedTextDto> parsedDataIterator = parsedData.listIterator();
        while (parsedDataIterator.hasNext()) {
            var data = parsedDataIterator.next();
            requiredKeys.stream()
                    .filter(dataEntri -> dataEntri.getNama().equals(data.getTextKey()))
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