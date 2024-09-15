package m2codes.perizinan_ocr_tool.application.util.data_extractor;

import java.util.List;

public class OrganizationNameExtractor extends DataExtractorTemplate implements TextDataExtractor {

    @Override
    protected String regexPattern() {
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