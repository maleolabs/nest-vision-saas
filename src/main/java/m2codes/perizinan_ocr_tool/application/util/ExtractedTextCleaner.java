package m2codes.perizinan_ocr_tool.application.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ExtractedTextCleaner {

    private static final String invalidCharsContain = "«#$^*;";

    public static String[] linesCleaner(String[] lines) {
        if (lines == null || lines.length == 0)
            return lines;

        StringBuilder builder = new StringBuilder();

        for (int i=0; i<lines.length; i++) {
            lines[i] = cleanLine(lines[i]);
            int lastIndex = i - 1;
            if (i > 0 && (startsWithLowerCase(lines[i]) || firstLineBracketsNotClosed(lines[lastIndex], lines[i]))) {
                appendLines(builder, lines[lastIndex], lines[i]);
                lines[lastIndex] = builder.toString();
                lines[i] = "";
                builder.setLength(0);
            }
        }

        return Arrays.stream(lines)
                .filter(line -> !line.isEmpty())
                .toArray(String[]::new);
    }

    private static boolean startsWithLowerCase(String text) {
        return !text.isEmpty() && Character.isLowerCase(text.charAt(0));
    }

    private static boolean firstLineBracketsNotClosed(String firstLine, String secondLine) {
        return firstLine.contains("(") && !firstLine.contains(")") && secondLine.contains(")");
    }

    private static String cleanLine(String line) {
        line = removeUnusedSpace(line);

        String[] words = line.split("\\s+");

        String invalidCharRegex = ".*[" + invalidCharsContain + "].*";

        line = Arrays.stream(words)
                .filter(word -> !word.matches(invalidCharRegex))
                .map(String::trim)
                .collect(Collectors.joining(" "));

        return line;
    }

    private static String removeUnusedSpace(String text) {
        text = text.trim();
        text = text.replaceAll("/\\s+", "/");
        text = text.replaceAll("\\s+/", "/");
        return text;
    }

    private static void appendLines(StringBuilder builder, String lastLines, String currentLines) {
        if (lastLines.endsWith("/"))
            builder.append(lastLines).append(currentLines);
        else
            builder.append(lastLines).append(" ").append(currentLines);
    }

}