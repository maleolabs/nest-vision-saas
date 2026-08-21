package m2codes.ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

@Slf4j
@Component
public class ExtractedTextMapper {

    private final CustomTextExtraction customTextExtraction;

    public ExtractedTextMapper(CustomTextExtraction customTextExtraction) {
        this.customTextExtraction = customTextExtraction;
    }

    public List<ExtractedTextDto> parseLinesByColon(String[] lines) {
        return Arrays.stream(lines)
                .map(this::robustSplit)
                .filter(parts -> parts != null && parts.length == 2)
                .map(parts -> {
                    String key = normalizeKey(parts[0].trim().toLowerCase());
                    String value = correctValue(parts[1].trim(), key);
                    return ExtractedTextDto.builder()
                            .textKey(key)
                            .textValue(value)
                            .build();
                }).filter(textDto -> !textDto.getTextValue().isEmpty() && !textDto.getTextValue().isBlank())
                .toList();
    }

    private String[] robustSplit(String line) {
        // Try multiple delimiters: colon, semicolon, pipe, double-space, dash
        String[] delimiters = {":", ";", "\\|", "\\s{2,}", "\\s+-\\s+"};
        for (String delim : delimiters) {
            String[] parts = line.split(delim, 2);
            if (parts.length == 2 && parts[0].trim().length() >= 2 && parts[1].trim().length() > 0 && parts[0].length() < 40) {
                return parts;
            }
        }
        // fallback colon
        return splitLine(line);
    }

    private String[] splitLine(String line) {
        // normalize common OCR misreads: · • -> :
        String normalized = line.replace("·", ":").replace("•", ":");
        return normalized.split(":", 2);
    }

    private String normalizeKey(String raw) {
        raw = raw.replaceAll("[:;\\|]+$", "").trim();
        // fuzzy normalize via known KTP keys
        java.util.List<String> known = java.util.List.of(
                "provinsi","kabupaten","kota","nik","nama","tempat/tgl lahir","jenis kelamin",
                "gol. darah","alamat","rt/rw","kelurahan","kecamatan","agama","status perkawinan",
                "pekerjaan","kewarganegaraan","berlaku hingga"
        );
        String best = raw;
        int bestDist = Integer.MAX_VALUE;
        for (String k : known) {
            int d = levenshtein(raw, k);
            if (d < bestDist && d <= 3) {
                bestDist = d;
                best = k;
            }
        }
        return bestDist <= 3 ? best : raw;
    }

    private String correctValue(String value, String key) {
        if (key.contains("nik")) {
            // P1.4 whitelist correction
            value = value.replace("O","0").replace("o","0").replace("I","1").replace("l","1").replace("L","1").replace("B","8").replace("S","5").replace("Z","2").replace(" ","").replace("-","");
            value = value.replaceAll("[^0-9]", "");
            if (value.length() > 16) value = value.substring(0,16);
        }
        return value;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length()+1][b.length()+1];
        for (int i=0;i<=a.length();i++) dp[i][0]=i;
        for (int j=0;j<=b.length();j++) dp[0][j]=j;
        for (int i=1;i<=a.length();i++) for(int j=1;j<=b.length();j++) {
            int cost = a.charAt(i-1)==b.charAt(j-1)?0:1;
            dp[i][j]=Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+cost);
        }
        return dp[a.length()][b.length()];
    }

    public List<ExtractedTextDto> detectAndAddMissingKeyValue(String[] lines, List<String> requiredKeys) {
        return requiredKeys.stream()
                .filter(requiredKey -> getCustomText(requiredKey, lines) != null)
                .map(requiredKeyFound -> ExtractedTextDto.builder()
                        .textKey(requiredKeyFound)
                        .textValue(getCustomText(requiredKeyFound, lines))
                        .build())
                .toList();
    }

    private String getCustomText(String category, String[] lines) {
        return customTextExtraction.findTextByCategory(category, lines);
    }

    public List<ExtractedTextDto> filterParsedDataByRequiredKeys(List<ExtractedTextDto> parsedData, List<String> requiredKeys) {
        if (requiredKeys == null || requiredKeys.isEmpty()) return parsedData;
        java.util.List<String> normalizedRequired = requiredKeys.stream().map(k -> k.toLowerCase().trim()).toList();
        ListIterator<ExtractedTextDto> it = parsedData.listIterator();
        while (it.hasNext()) {
            var data = it.next();
            String dataKeyNorm = data.getTextKey().toLowerCase().trim();
            // exact match first (case-insensitive)
            var exact = normalizedRequired.stream().filter(k -> k.equals(dataKeyNorm)).findFirst();
            if (exact.isPresent()) {
                // restore original casing from requiredKeys
                int idx = normalizedRequired.indexOf(exact.get());
                data.setTextKey(requiredKeys.get(idx));
                it.set(data);
                continue;
            }
            // fuzzy match levenshtein <=3
            String best = null;
            int bestDist = Integer.MAX_VALUE;
            for (int i=0;i<normalizedRequired.size();i++) {
                int d = levenshtein(dataKeyNorm, normalizedRequired.get(i));
                if (d < bestDist && d <= 3) {
                    bestDist = d;
                    best = requiredKeys.get(i);
                }
            }
            if (best != null) {
                data.setTextKey(best);
                it.set(data);
            }
        }
        return parsedData;
    }

}