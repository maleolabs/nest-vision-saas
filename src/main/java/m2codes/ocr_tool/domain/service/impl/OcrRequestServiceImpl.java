package m2codes.ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.RequestStatus;
import org.springframework.stereotype.Service;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Service
public class OcrRequestServiceImpl implements OcrRequestService {

    private final OcrRequestRepository ocrRequestRepository;

    public OcrRequestServiceImpl(OcrRequestRepository ocrRequestRepository) {
        this.ocrRequestRepository = ocrRequestRepository;
    }

    @Override
    public OcrRequest save(OcrDataRequest request, RequestStatus status, String clientId) {
        var imageUrl = request != null ? request.getImageUrl() : null;

        UUID savedImageUploadId = ocrRequestRepository
                .findFirstByImageUrl(imageUrl)
                .map(OcrRequest::getId).orElse(null);

        OcrRequest ocrRequest = OcrRequest.builder()
                .id(savedImageUploadId)
                .imageUrl(imageUrl)
                .status(status)
                .clientId(clientId)
                .build();

        return ocrRequestRepository.save(ocrRequest);
    }

    @Override
    public Optional<OcrRequest> find(String id) {
        UUID requestId = UUID.fromString(id);
        return ocrRequestRepository.findById(requestId);
    }

    @Override
    public void updateStatus(OcrRequest request, RequestStatus status) {
        request.setStatus(status);
        ocrRequestRepository.save(request);
    }

    @Override
    public boolean existsByImageUrlAndClientId(String imageUrl, String clientId) {
        return ocrRequestRepository.existsByImageUrlAndClientId(imageUrl, clientId);
    }
}