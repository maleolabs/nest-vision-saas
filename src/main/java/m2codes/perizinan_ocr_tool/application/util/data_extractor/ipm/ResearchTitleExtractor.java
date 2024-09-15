package m2codes.perizinan_ocr_tool.application.util.data_extractor.ipm;

import m2codes.perizinan_ocr_tool.application.util.data_extractor.DataExtractorTemplate;
import m2codes.perizinan_ocr_tool.application.util.data_extractor.TextDataExtractor;

import java.util.List;

public class ResearchTitleExtractor extends DataExtractorTemplate implements TextDataExtractor {

    @Override
    public boolean textExist(String ocrText) {
        return super.textExist(ocrText.toLowerCase());
    }

    @Override
    protected String regexPattern() {
        StringBuilder builder = new StringBuilder();
        for (String key : similarKeys()) {
            builder.append(key).append("|");
        }
        return builder.substring(0, builder.length() - 1) + "\\s+([A-Za-z\\s:?().]+)";
    }

    private List<String> similarKeys() {
        return List.of(
                "judul"
        );
    }

}