package m2codes.ocr_tool.domain.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.RequestStatus;
import org.springframework.stereotype.Service;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;

import java.time.Instant;
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

        // XR-07: dedupe harus scoped per client — sebelumnya findFirstByImageUrl lintas client
        // membuat status/hasil request bisa dipakai ulang oleh client lain. Bila clientId kosong,
        // reuse di-skip (selalu buat record baru) supaya tidak memutasi baris milik client lain.
        // Trade-off (pre-existing): dua request concurrent (imageUrl, clientId) sama bisa
        // membuat 2 row (racenya antara lookup & save, tanpa unique constraint di DB);
        // findFirst juga nondeterministik di antara duplikat history. Failure-path worker
        // tidak lagi lewat sini (pakai updateStatus entity) supaya tidak salah sasaran.
        UUID savedImageUploadId = (imageUrl != null && clientId != null && !clientId.isBlank())
                ? ocrRequestRepository
                        .findFirstByImageUrlAndClientId(imageUrl, clientId)
                        .map(OcrRequest::getId).orElse(null)
                : null;

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
        // status terminal -> catat wall-clock selesai; dipakai UI menampilkan
        // total waktu (requestedAt -> completedAt), bukan hanya waktu engine OCR.
        if (status == RequestStatus.DONE || status == RequestStatus.FAILURE) {
            request.setCompletedAt(Instant.now());
        }
        ocrRequestRepository.save(request);
    }

    @Override
    public boolean existsByImageUrlAndClientId(String imageUrl, String clientId) {
        return ocrRequestRepository.existsByImageUrlAndClientId(imageUrl, clientId);
    }
}