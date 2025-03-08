package m2codes.ocr_tool.domain.service;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;

import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrRequestService {

    OcrRequest save(OcrDataRequest request, RequestStatus status, String clientId);

    Optional<OcrRequest> find(String id);

    void updateStatus(OcrRequest request, RequestStatus status);

}