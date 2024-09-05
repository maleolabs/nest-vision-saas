package m2codes.perizinan_ocr_tool.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDto {

    private String extractedText;

    private boolean isSuccess;

    private String errorMessage;

    private Long extractedAt;

    @Override
    public String toString() {
        return "OcrResultDto{" +
                "extractedText=" + (extractedText.length() > 25 ? extractedText.substring(0,25) : extractedText) +
                ", isSuccess=" + isSuccess +
                ", errorMessage=" + errorMessage +
                ", extractedAt=" + extractedAt +
                "}";
    }
}