package m2codes.ocr_tool.application.dto;

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

    private Long duration;

    // Observability fields (P3.11)
    private Integer confidence; // 0-100
    private Double blurScore; // Laplacian variance
    private Double brightness;
    private Double contrast;
    private String psmUsed;
    private String engineUsed; // tesseract, paddle, llm
    private boolean superResolutionApplied;

    @Override
    public String toString() {
        return "OcrResultDto{" +
                "extractedText=" + (extractedText != null && extractedText.length() > 25 ? extractedText.substring(0,25) : extractedText) +
                ", isSuccess=" + isSuccess +
                ", errorMessage=" + errorMessage +
                ", duration=" + duration +
                ", confidence=" + confidence +
                ", blurScore=" + blurScore +
                ", engine=" + engineUsed +
                "}";
    }
}