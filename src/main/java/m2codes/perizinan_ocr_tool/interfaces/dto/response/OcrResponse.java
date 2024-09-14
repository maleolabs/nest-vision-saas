package m2codes.perizinan_ocr_tool.interfaces.dto.response;

import lombok.*;
import m2codes.perizinan_ocr_tool.domain.model.RequestStatus;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OcrResponse {

    private Long requestId;

    private RequestStatus status;

}