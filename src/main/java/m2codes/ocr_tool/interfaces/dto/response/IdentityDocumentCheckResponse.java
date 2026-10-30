package m2codes.ocr_tool.interfaces.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class IdentityDocumentCheckResponse {
    private String requestId;
    private String status;
    private String documentType;
    private String decision;
    private String reasonCode;
    /**
     * Field hasil ekstraksi OCR dari pass tunggal verifikasi (key/value).
     * Kosong saat status belum DONE atau verifikasi gagal.
     */
    private List<ExtractedTextResponse> extractedFields;
}
