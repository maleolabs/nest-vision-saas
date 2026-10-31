package m2codes.ocr_tool.interfaces.dto.response;

import lombok.*;
import m2codes.ocr_tool.domain.model.RequestStatus;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrResponse2 {

    private String requestId;

    private String clientId;

    private RequestStatus status;

    private Float duration;

    // Statistik observability (sama dengan OcrResponse, untuk konsistensi API).
    private Integer confidence;
    private Double blurScore;
    private Double brightness;
    private Double contrast;
    private String psmUsed;
    private String engineUsed;
    private Boolean superResolutionApplied;

    private Map<String, String> extractedTexts;

}