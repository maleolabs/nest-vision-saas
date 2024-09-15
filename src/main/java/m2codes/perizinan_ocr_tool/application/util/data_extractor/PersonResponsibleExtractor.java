package m2codes.perizinan_ocr_tool.application.util.data_extractor;

public class PersonResponsibleExtractor extends DataExtractorTemplate implements TextDataExtractor {

    @Override
    public String extract(String ocrText) {
        return null;
    }

    @Override
    public boolean textExist(String ocrText) {
        return false;
    }

    @Override
    protected String regexPattern() {
        return "";
    }

}