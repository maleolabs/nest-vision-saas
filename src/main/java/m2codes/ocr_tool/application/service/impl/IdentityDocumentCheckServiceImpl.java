package m2codes.ocr_tool.application.service.impl;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.application.service.IdentityDocumentCheckService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.infrastructure.filemanager.service.FileManagerService;
import m2codes.ocr_tool.interfaces.dto.response.IdentityDocumentCheckResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class IdentityDocumentCheckServiceImpl implements IdentityDocumentCheckService {
    private final OcrRequestRepository requests;
    private final FileManagerService files;
    private final IdentityDocumentCheckWorker worker;

    @Override
    public IdentityDocumentCheckResponse submit(MultipartFile image, String clientId) {
        if (image == null || image.isEmpty() || clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("image and clientId are required");
        }
        File file = null;
        try {
            String identifier = files.uploadFile(image);
            OcrRequest request = requests.save(OcrRequest.builder().imageUrl(identifier)
                    .clientId(clientId).status(RequestStatus.PROCESSING).build());
            file = files.createTempFileFromInputStream(files.retrieveFile(identifier), identifier);
            worker.process(request.getId(), clientId, file);
            return response(request);
        } catch (Exception e) {
            throw new IllegalArgumentException("image upload failed", e);
        }
    }

    @Override public IdentityDocumentCheckResponse get(String id, String clientId) {
        try { return requests.findByIdAndClientId(UUID.fromString(id), clientId).map(this::response).orElse(null); }
        catch (IllegalArgumentException e) { return null; }
    }

    private IdentityDocumentCheckResponse response(OcrRequest r) { return IdentityDocumentCheckResponse.builder()
            .requestId(r.getId().toString()).status(r.getStatus().name()).documentType(r.getIdentityDocumentType())
            .decision(r.getIdentityDecision()).reasonCode(r.getIdentityReasonCode()).build(); }

    static String classify(String text) {
        if (text == null) return null;
        text = text.toUpperCase(Locale.ROOT);
        if (text.matches("(?s).*\\b(KTP|KARTU TANDA PENDUDUK)\\b.*")) return "KTP";
        if (text.matches("(?s).*\\b(SIM|SURAT IZIN MENGEMUDI)\\b.*")) return "SIM";
        return null;
    }

    static boolean hasRequiredIdentityFields(String text, String type) {
        String normalized = text == null ? "" : text.toUpperCase(Locale.ROOT);
        if ("KTP".equals(type)) return normalized.matches("(?s).*\\b\\d{16}\\b.*")
                && normalized.matches("(?s).*\\bNAMA\\b.*");
        return normalized.matches("(?s).*\\b(NAMA|NAME)\\b.*")
                && normalized.matches("(?s).*\\b(SIM|SURAT IZIN MENGEMUDI)\\b.*");
    }
}
