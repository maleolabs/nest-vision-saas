package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.interfaces.dto.response.IdentityDocumentCheckResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IdentityDocumentCheckService {
    IdentityDocumentCheckResponse submit(MultipartFile image, String clientId, List<String> requiredKeys);
    IdentityDocumentCheckResponse get(String requestId, String clientId);
}
