package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.interfaces.dto.response.IdentityDocumentCheckResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IdentityDocumentCheckService {
    IdentityDocumentCheckResponse submit(MultipartFile image, String clientId);
    IdentityDocumentCheckResponse get(String requestId, String clientId);
}
