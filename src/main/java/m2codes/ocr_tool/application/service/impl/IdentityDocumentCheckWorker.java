package m2codes.ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.UUID;

@Slf4j @Service
public class IdentityDocumentCheckWorker {
    private final TextExtractionService ocr;
    private final OcrRequestRepository requests;

    public IdentityDocumentCheckWorker(
            @Qualifier("ensembleOcrService") TextExtractionService ocr,
            OcrRequestRepository requests) {
        this.ocr = ocr;
        this.requests = requests;
    }

    @Async("ocrTaskExecutor")
    public void process(UUID id, String clientId, File file) {
        OcrRequest r = requests.findByIdAndClientId(id, clientId).orElse(null);
        if (r == null) return;
        try {
            OcrResultDto result = ocr.extractTextFromImage(file, false);
            String text = result == null || result.getExtractedText() == null ? "" : result.getExtractedText();
            String type = IdentityDocumentCheckServiceImpl.classify(text);
            r.setStatus(RequestStatus.DONE); r.setIdentityDocumentType(type);
            if (type == null) {
                r.setIdentityDecision("REJECT");
                r.setIdentityReasonCode("UNSUPPORTED_DOCUMENT");
            } else if (IdentityDocumentCheckServiceImpl.hasRequiredIdentityFields(text, type)) {
                r.setIdentityDecision("ACCEPTED");
                r.setIdentityReasonCode("DOCUMENT_VALID");
            } else {
                r.setIdentityDecision("REVIEW");
                r.setIdentityReasonCode("DOCUMENT_AMBIGUOUS");
            }
        } catch (Exception e) {
            log.warn("Identity document OCR failed for {}", id, e);
            r.setStatus(RequestStatus.FAILURE); r.setIdentityDecision("REJECT"); r.setIdentityReasonCode("OCR_FAILED");
        }
        finally {
            requests.save(r);
            if (file.exists() && !file.delete()) file.deleteOnExit();
        }
    }
}
