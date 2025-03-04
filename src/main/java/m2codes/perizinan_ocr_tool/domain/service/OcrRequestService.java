package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.model.RequestStatus;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author marij_mokoginta
 */
public interface OcrRequestService {

    OcrRequest save(OcrDataRequest request, RequestStatus status);

    Optional<OcrRequest> find(Long id);

    void updateStatus(OcrRequest request, RequestStatus status);

}