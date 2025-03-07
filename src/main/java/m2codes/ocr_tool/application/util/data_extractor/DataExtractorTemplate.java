package m2codes.ocr_tool.application.util.data_extractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DataExtractorTemplate {

    public String extract(String ocrText) {
        var matcher = textMatcher(ocrText);
        return matcher.find() ? matcher.group(0) : null;
    }

    public boolean textExist(String ocrText) {
        return textMatcher(ocrText).find();
    }

    protected final Matcher textMatcher(String ocrText) {
        Pattern pattern = Pattern.compile(regexPattern());
        return pattern.matcher(ocrText);
    }

    protected abstract String regexPattern();

}