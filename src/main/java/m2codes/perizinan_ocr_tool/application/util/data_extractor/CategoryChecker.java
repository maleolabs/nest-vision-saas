package m2codes.perizinan_ocr_tool.application.util.data_extractor;

import java.util.regex.Pattern;

public class CategoryChecker {

    public static boolean isResearchTitleCategory(String category) {
        return category.contains("judul penelitian");
    }

    public static boolean isMatch(String category, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern);
        return pattern.matcher(category).find();
    }

}
