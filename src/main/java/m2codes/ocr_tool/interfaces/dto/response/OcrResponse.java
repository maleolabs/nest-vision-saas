package m2codes.ocr_tool.interfaces.dto.response;

import lombok.*;
import m2codes.ocr_tool.domain.model.RequestStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrResponse {

    private Long requestId;

    private RequestStatus status;

    private Float duration;

    private String originalExtractedText;

    private List<ExtractedTextResponse> extractedTexts = new ArrayList<>();

}