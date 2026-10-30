package m2codes.ocr_tool.application.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.ocr_tool.application.util.ExtractedTextMapper;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j @Service
public class IdentityDocumentCheckWorker {
    private final TextExtractionService ocr;
    private final OcrRequestRepository requests;
    private final OcrResultService ocrResults;
    private final ExtractedTextService extractedTexts;
    private final ExtractedTextCleaner extractedTextCleaner;
    private final ExtractedTextMapper extractedTextMapper;

    public IdentityDocumentCheckWorker(
            @Qualifier("ensembleOcrService") TextExtractionService ocr,
            OcrRequestRepository requests,
            OcrResultService ocrResults,
            ExtractedTextService extractedTexts,
            ExtractedTextCleaner extractedTextCleaner,
            ExtractedTextMapper extractedTextMapper) {
        this.ocr = ocr;
        this.requests = requests;
        this.ocrResults = ocrResults;
        this.extractedTexts = extractedTexts;
        this.extractedTextCleaner = extractedTextCleaner;
        this.extractedTextMapper = extractedTextMapper;
    }

    @Async("ocrTaskExecutor")
    @Transactional
    public void process(UUID id, String clientId, File file, List<String> requiredKeys) {
        OcrRequest r = requests.findByIdAndClientId(id, clientId).orElse(null);
        if (r == null) return;
        try {
            OcrResultDto result = ocr.extractTextFromImage(file, false);
            boolean ocrSuccess = result != null && result.isSuccess();
            if (!ocrSuccess) {
                r.setStatus(RequestStatus.FAILURE); r.setIdentityDecision("REJECT");
                r.setIdentityReasonCode("OCR_FAILED");
                requests.save(r);
                return;
            }
            String text = result.getExtractedText() != null ? result.getExtractedText() : "";

            OcrResult savedResult = persistOcrResult(result, r, text, requiredKeys);

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
            requests.save(r);
            log.info("[IdentityCheck] req={} done type={} decision={} extractedFields={}",
                    id, type, r.getIdentityDecision(),
                    savedResult != null && savedResult.getExtractedText() != null ? savedResult.getExtractedText().size() : 0);
        } catch (Exception e) {
            log.warn("Identity document OCR failed for {}", id, e);
            r.setStatus(RequestStatus.FAILURE); r.setIdentityDecision("REJECT"); r.setIdentityReasonCode("OCR_FAILED");
            requests.save(r);
        }
        finally {
            if (file.exists() && !file.delete()) file.deleteOnExit();
        }
    }

    /**
     * Simpan hasil OCR pass tunggal (OcrResult + parsed ExtractedText) ke baris
     * OcrRequest yang sama. Web cukup GET ulang endpoint identity untuk mengambil
     * fields — tidak perlu submit /ocr/do-ocr kedua.
     */
    private OcrResult persistOcrResult(OcrResultDto result, OcrRequest r, String text, List<String> requiredKeys) {
        try {
            OcrResult ocrResult = ocrResults.save(result, r);
            List<ExtractedTextDto> parsed = parseFields(text, requiredKeys);
            if (!parsed.isEmpty()) {
                extractedTexts.saveAll(parsed, ocrResult);
            }
            return ocrResult;
        } catch (Exception e) {
            log.warn("[IdentityCheck] persist OCR result failed for {} (decision tidak terpengaruh)", r.getId(), e);
            return null;
        }
    }

    private List<ExtractedTextDto> parseFields(String text, List<String> requiredKeys) {
        String[] lines = text.split("\\r?\\n");
        String[] cleanLines = extractedTextCleaner.linesCleaner(lines);
        List<ExtractedTextDto> dtos = new java.util.ArrayList<>(extractedTextMapper.parseLinesByColon(cleanLines));
        dtos.addAll(extractedTextMapper.detectAndAddMissingKeyValue(cleanLines, requiredKeys));
        List<ExtractedTextDto> filtered = extractedTextMapper.filterParsedDataByRequiredKeys(dtos, requiredKeys);
        log.info("[IdentityCheck] parsed {} fields, filtered {} (requiredKeys={})",
                dtos.size(), filtered.size(), requiredKeys != null ? requiredKeys.size() : 0);
        return filtered;
    }
}
