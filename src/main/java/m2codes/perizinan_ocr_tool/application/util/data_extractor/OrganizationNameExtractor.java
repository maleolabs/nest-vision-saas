package m2codes.perizinan_ocr_tool.application.util.data_extractor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrganizationNameExtractor implements TextDataExtractor {

    @Override
    public String extract(String ocrText) {
        var matcher = textMatcher(ocrText);
        return matcher.find() ? matcher.group(0) : null;
    }

    @Override
    public boolean textExist(String ocrText) {
        return textMatcher(ocrText).find();
    }

    private Matcher textMatcher(String text) {
        Pattern pattern = Pattern.compile(regexPattern());
        return pattern.matcher(text);
    }

    private String regexPattern() {
        StringBuilder regexBuilder = new StringBuilder();
        for (String organization : listOfOrganization()) {
            regexBuilder.append(organization).append("|");
        }
        return regexBuilder.substring(0, regexBuilder.length() - 1) + "\\s+([A-Za-z\\s]+)";
    }

    private List<String> listOfOrganization() {
        return List.of(
            "UNIVERSITAS", "INSTITUT", "POLITEKNIK", "SEKOLAH",
            "DINAS", "PUSKESMAS", "PEMERINTAH"
        );
    }

}