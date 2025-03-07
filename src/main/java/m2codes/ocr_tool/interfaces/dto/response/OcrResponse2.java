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

    private Long requestId;

    private RequestStatus status;

    private Float duration;

    private Map<String, String> extractedTexts;

}