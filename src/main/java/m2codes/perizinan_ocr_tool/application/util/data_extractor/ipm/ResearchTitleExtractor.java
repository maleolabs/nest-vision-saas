package m2codes.perizinan_ocr_tool.application.util.data_extractor.ipm;

import m2codes.perizinan_ocr_tool.application.util.data_extractor.DataExtractorTemplate;
import m2codes.perizinan_ocr_tool.application.util.data_extractor.TextDataExtractor;

public class ResearchTitleExtractor extends DataExtractorTemplate implements TextDataExtractor {

    @Override
    protected String regexPattern() {
        return "^Judul|([A-Z][a-zA-Z]+(\\s\\(?[a-zA-Z]+\\)?)?{2,9})(\\.|,)?$\n";
    }

}