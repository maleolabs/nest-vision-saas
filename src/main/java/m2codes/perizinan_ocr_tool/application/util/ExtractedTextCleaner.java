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
        int openQuoteIndex = 0;
        boolean isQuoteNotClosed = false;

        for (int i = 0; i < lines.length; i++) {
            int previousIndex = i - 1;
            lines[i] = cleanQuotes(lines[i]);

            if (i > 0 && lines[previousIndex].contains("\"")) {
                lines[previousIndex] = combineLinesWithQuotes(lines[previousIndex], lines[i]);
                if (!lines[previousIndex].contains("\"")) {
                    isQuoteNotClosed = true;
                    openQuoteIndex = previousIndex;
                }
            }

            if (isQuoteNotClosed && lines[i].contains("\"")) {
                lines[openQuoteIndex] = closeOpenQuotes(lines[openQuoteIndex], lines[i]);
                lines[i] = "";
                isQuoteNotClosed = false;
            }

            lines[i] = removeNoise(lines[i]);

            if (i > 0 && !isQuoteNotClosed && isSentenceContinuation(lines[previousIndex], lines[i])) {
                appendLines(builder, lines[previousIndex], lines[i]);
                lines[previousIndex] = builder.toString();
                lines[i] = "";
                builder.setLength(0);
            }
        }

        return Arrays.stream(lines)
                .filter(line -> !line.isEmpty())
                .toArray(String[]::new);
    }

    private String cleanQuotes(String text) {
        if (!text.contains("\""))
            return text;
        String quoptrn = "(\"[a-zA-Z0-9]+\")|(\"[a-zA-Z0-9]+)|([a-zA-Z0-9]+\")";
        return text.matches(quoptrn) ? text : "";
    }

    private boolean isSentenceContinuation(String previousLine, String currentLine) {
        return startsWithLowerCase(previousLine) || areLinesRelated(previousLine, currentLine);
    }

    private boolean startsWithLowerCase(String text) {
        return !text.isEmpty() && Character.isLowerCase(text.charAt(0));
    }

    private boolean areLinesRelated(String previousLine, String currentLine) {
        return previousLine.contains("(") && !previousLine.contains(")") && currentLine.contains(")");
    }

    private String removeNoise(String line) {
        line = removeUnusedSpace(line);

        String validRegex = ".*[^A-Za-z0-9:/.\s()'].*";

        String[] words = line.split("\\s+");
        return Arrays.stream(words)
                .filter(word -> !word.matches(validRegex) && isValidWord(word))
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private boolean isValidWord(String word) {
        return word.contains(":") || word.length() > 2;
    }

    private String removeUnusedSpace(String text) {
        return text.trim().replaceAll("/\\s+", "/").replaceAll("\\s+/", "/");
    }

    private void appendLines(StringBuilder builder, String lastLine, String currentLine) {
        if (lastLine.endsWith("/"))
            builder.append(lastLine).append(currentLine);
        else
            builder.append(lastLine).append(" ").append(currentLine);
    }

    private String combineLinesWithQuotes(String previousLine, String currentLine) {
        String[] prevLineParts = previousLine.split("\"");
        String[] currentLineParts = currentLine.split("\"");
        return prevLineParts[1] + (currentLine.contains("\"") ? currentLineParts[0] : "");
    }

    private String closeOpenQuotes(String openQuoteLine, String currentLine) {
        String[] currentLineParts = currentLine.split("\"");
        return openQuoteLine + currentLineParts[0];
    }

}