package m2codes.ocr_tool.application.util;

import m2codes.ocr_tool.application.util.data_extractor.OrganizationNameExtractor;
import m2codes.ocr_tool.application.util.data_extractor.PersonResponsibleExtractor;
import m2codes.ocr_tool.application.util.data_extractor.TextDataExtractor;
import m2codes.ocr_tool.application.util.data_extractor.ipm.ResearchTitleExtractor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static m2codes.ocr_tool.application.util.data_extractor.CategoryChecker.isMatch;
import static m2codes.ocr_tool.application.util.data_extractor.CategoryChecker.isResearchTitleCategory;

@Component
public class CustomTextExtraction {

    public String findTextByCategory(String category, String[] lines) {
        var extractor = getExtractor(category);
        if (extractor == null)
            return null;

        return Arrays.stream(lines)
                .filter(extractor::textExist)
                .findFirst()
                .map(line -> {
                    if (line.contains(":")) {
                        return line.split(":", 2)[1].trim();
                    }
                    return line;
                })
                .orElse(null);
    }

    private TextDataExtractor getExtractor(String category) {
        if (isOrganizationCategory(category)) {
            return new OrganizationNameExtractor();
        } else if (isPersonResponsible(category)) {
            return new PersonResponsibleExtractor();
        } else if (isResearchTitleCategory(category)) {
            return new ResearchTitleExtractor();
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

}