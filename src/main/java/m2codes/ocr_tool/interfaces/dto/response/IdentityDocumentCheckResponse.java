package m2codes.ocr_tool.interfaces.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdentityDocumentCheckResponse {
    private String requestId;
    private String status;
    private String documentType;
    private String decision;
    private String reasonCode;
}
