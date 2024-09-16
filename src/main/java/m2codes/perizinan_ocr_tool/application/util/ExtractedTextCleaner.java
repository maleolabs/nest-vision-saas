package m2codes.perizinan_ocr_tool.application.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class ExtractedTextCleaner {

    public String[] linesCleaner(String[] lines) {
        if (lines == null || lines.length == 0)
            return lines;

        StringBuilder builder = new StringBuilder();

        for (int i=0; i<lines.length; i++) {
            lines[i] = removeNoise(lines[i]);
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

    private boolean startsWithLowerCase(String text) {
        return !text.isEmpty() && Character.isLowerCase(text.charAt(0));
    }

    private boolean firstLineBracketsNotClosed(String firstLine, String secondLine) {
        return firstLine.contains("(") && !firstLine.contains(")") && secondLine.contains(")");
    }

    private String removeNoise(String line) {
        line = removeUnusedSpace(line);

        String validRegex = ".*[^A-Za-z0-9:/.\s()'].*";

        String[] words = line.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> !word.matches(validRegex) && wordValid(word))
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private boolean wordValid(String word) {
        return word.contains(":") || word.length() > 2;
    }

    private String removeUnusedSpace(String text) {
        text = text.trim();
        text = text.replaceAll("/\\s+", "/");
        text = text.replaceAll("\\s+/", "/");
        return text;
    }

    private void appendLines(StringBuilder builder, String lastLines, String currentLines) {
        if (lastLines.endsWith("/"))
            builder.append(lastLines).append(currentLines);
        else
            builder.append(lastLines).append(" ").append(currentLines);
    }

}