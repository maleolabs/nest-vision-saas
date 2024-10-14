package m2codes.perizinan_ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.RequestStatus;
import org.springframework.stereotype.Service;

import m2codes.perizinan_ocr_tool.domain.model.OcrRequest;
import m2codes.perizinan_ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.perizinan_ocr_tool.domain.service.OcrRequestService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;

import java.util.List;
import java.util.Optional;

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
    public OcrRequest save(OcrDataRequest request, RequestStatus status) {
        var izinId = request != null ? request.getIzinId() : null;
        var syaratIzinId = request != null ? request.getSyaratIzinId() : null;
        var jenisPerizinanId = request != null ? request.getJenisPerizinanId() : null;
        var imageUrl = request != null ? request.getImageUrl() : null;

        Long savedImageUploadId = ocrRequestRepository
                .findFirstByIzinIdAndSyaratIzinId(izinId, syaratIzinId)
                .map(OcrRequest::getId).orElse(null);

        OcrRequest ocrRequest = OcrRequest.builder()
                .id(savedImageUploadId)
                .izinId(izinId)
                .jenisPerizinanId(jenisPerizinanId)
                .syaratIzinId(syaratIzinId)
                .imageUrl(imageUrl)
                .requestedAt(System.currentTimeMillis())
                .status(status)
                .build();

        return ocrRequestRepository.save(ocrRequest);
    }

    @Override
    public Optional<OcrRequest> find(Long id) {
        return ocrRequestRepository.findById(id);
    }

    @Override
    public List<OcrRequest> findByIzinId(Long izinId) {
        return ocrRequestRepository.findByIzinId(izinId);
    }

    @Override
    public Optional<OcrRequest> findFirstByIzinIdAndSyaratIzinId(Long izinId, Long syaratIzinId) {
        return ocrRequestRepository.findFirstByIzinIdAndSyaratIzinId(izinId, syaratIzinId);
    }

    @Override
    public void updateStatus(OcrRequest request, RequestStatus status) {
        request.setStatus(status);
        ocrRequestRepository.save(request);
    }
}