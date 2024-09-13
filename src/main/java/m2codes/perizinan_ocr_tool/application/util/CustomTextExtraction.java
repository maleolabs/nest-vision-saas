package m2codes.perizinan_ocr_tool.application.util;

import m2codes.perizinan_ocr_tool.application.util.data_extractor.OrganizationNameExtractor;
import m2codes.perizinan_ocr_tool.application.util.data_extractor.PersonResponsibleExtractor;
import m2codes.perizinan_ocr_tool.application.util.data_extractor.TextDataExtractor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

@Component
public class CustomTextExtraction {

    public String findTextByCategory(String category, String[] lines) {
        var extractor = getExtractor(category);

        if (extractor == null)
            return null;

        return Arrays.stream(lines)
                .filter(extractor::textExist)
                .findFirst()
                .orElse(null);
    }

    private TextDataExtractor getExtractor(String category) {
        if (isOrganizationCategory(category)) {
            return new OrganizationNameExtractor();
        } else if (isPersonResponsible(category)) {
            return new PersonResponsibleExtractor();
        }
        return null;
    }

    private boolean isOrganizationCategory(String category) {
        String regexPattern = ".*\binstansi|lembaga|organisasi\b.*";
        return isMatch(category, regexPattern);
    }

    private boolean isPersonResponsible(String category) {
        String regexPattern = ".*\bpenanggung\s?jawab|atas\s?nama|penanda\s?tangan\b.*";
        return isMatch(category, regexPattern);
    }

    private boolean isMatch(String category, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        return pattern.matcher(category).find();
    }

}