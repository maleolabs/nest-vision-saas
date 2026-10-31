package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.domain.model.ExtractedText;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.OcrResult;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrRequestService;
import m2codes.ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse;
import m2codes.ocr_tool.interfaces.dto.response.OcrResponse2;
import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;

import java.util.*;

/**
 * Base template untuk query hasil OCR.
 *
 * <p><b>Proxy-safety:</b> saat ini {@link m2codes.ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl}
 * TIDAK di-advise (@Async/@Transactional/@Cacheable tidak ada) sehingga Spring tidak
 * membuat proxy dan {@code final} sebenarnya aman. Method entry point tetap sengaja
 * dibuat non-{@code final}: begitu subclass-nya diberi @Transactional/@Cacheable/@Async,
 * CGLIB akan mem-proxy via Objenesis (constructor tidak dipanggil, field dependency null)
 * dan method final tidak bisa di-override — pemanggilan lewat proxy akan mengeksekusi
 * implementasi base pada instance proxy yang dependency-nya null. Lihat komentar di
 * {@link TextProcessorService} untuk detail kasus nyata.
 */
public abstract class ExtractedTextQueryService {

    protected final ExtractedTextService extractedTextService;

    protected final OcrRequestService ocrRequestService;

    public ExtractedTextQueryService(
            ExtractedTextService extractedTextService,
            OcrRequestService ocrRequestService
    ) {
        this.extractedTextService = extractedTextService;
        this.ocrRequestService = ocrRequestService;
    }

    public ApiResponse<OcrResponse> checkStatus(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return builResponse(null, false, "Request with id : " + requestId + " not found!");
        }
        OcrResponse response = OcrResponse.builder()
                .requestId(requestId)
                .clientId(request.getClientId())
                .status(request.getStatus())
                .build();
        if (request.getOcrResults() != null) {
            response.setDuration(totalDurationSeconds(request));
        }
        return builResponse(response, true, null);
    }

    public ApiResponse<OcrResponse2> getByRequestId(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return builResponse(null, false, "Request ID not found");
        }
        var ocrResponse = OcrResponse2.builder()
                .requestId(request.getId().toString())
                .status(request.getStatus())
                .clientId(request.getClientId())
                .build();

        if (request.getOcrResults() != null) {
            var ocrResult = request.getOcrResults();
            ocrResponse.setDuration(totalDurationSeconds(request));
            fillStatistics(ocrResponse, ocrResult);
            Map<String, String> extractedTexts = new HashMap<>();
            for (ExtractedText extractedText : ocrResult.getExtractedText()) {
                extractedTexts.put(extractedText.getTextKey(), extractedText.getTextValue());
            }
            ocrResponse.setExtractedTexts(extractedTexts);
        }
        return builResponse(ocrResponse, true, null);
    }

    public OcrResponse findByRequestId(String requestId) {
        OcrRequest request = findRequestById(requestId);
        if (request == null) {
            return null;
        }
        var ocrResponse = OcrResponse.builder()
                .requestId(request.getId().toString())
                .status(request.getStatus())
                .clientId(request.getClientId())
                .build();
        if (request.getOcrResults() != null) {
            var ocrResult = request.getOcrResults();
            ocrResponse.setDuration(totalDurationSeconds(request));
            ocrResponse.setOriginalExtractedText(ocrResult.getOriginalExtractedText());
            fillStatistics(ocrResponse, ocrResult);

            List<ExtractedTextResponse> extractedTexts = new ArrayList<>();
            for (ExtractedText extractedText : ocrResult.getExtractedText()) {
                var extractedTextRes = ExtractedTextResponse.builder()
                        .textKey(extractedText.getTextKey())
                        .textValue(extractedText.getTextValue())
                        .build();
                extractedTexts.add(extractedTextRes);
            }
            ocrResponse.setExtractedTexts(extractedTexts);
        }
        return ocrResponse;
    }

    protected abstract <T> ApiResponse<T> builResponse(T data, boolean success, String errorMessage);

    protected abstract OcrRequest findRequestById(String requestId);

    /**
     * Total wall-clock pemrosesan (requestedAt -> completedAt) dalam detik.
     * Sebelumnya UI menampilkan OcrResult.duration (waktu 1 pass OCR engine saja)
     * yang menyesatkan: 25.0s padahal user menunggu ~3 menit (queue + OCR + parse).
     */
    protected Float totalDurationSeconds(OcrRequest request) {
        if (request == null || request.getRequestedAt() == null || request.getCompletedAt() == null) {
            return null;
        }
        return (float) java.time.Duration.between(request.getRequestedAt(), request.getCompletedAt()).toMillis() / 1000f;
    }

    /**
     * Isi statistik observability OCR (conf, blur, brightness, contrast, psm,
     * engine, SR) ke response. Data ini sudah dipersist OcrResultServiceImpl
     * sejak V10 — sebelumnya hanya tampil di log aplikasi.
     */
    protected void fillStatistics(OcrResponse response, OcrResult ocrResult) {
        response.setConfidence(ocrResult.getConfidence());
        response.setBlurScore(ocrResult.getBlurScore());
        response.setBrightness(ocrResult.getBrightness());
        response.setContrast(ocrResult.getContrast());
        response.setPsmUsed(ocrResult.getPsmUsed());
        response.setEngineUsed(ocrResult.getEngineUsed());
        response.setSuperResolutionApplied(ocrResult.getSuperResolutionApplied());
    }

    protected void fillStatistics(OcrResponse2 response, OcrResult ocrResult) {
        response.setConfidence(ocrResult.getConfidence());
        response.setBlurScore(ocrResult.getBlurScore());
        response.setBrightness(ocrResult.getBrightness());
        response.setContrast(ocrResult.getContrast());
        response.setPsmUsed(ocrResult.getPsmUsed());
        response.setEngineUsed(ocrResult.getEngineUsed());
        response.setSuperResolutionApplied(ocrResult.getSuperResolutionApplied());
    }

}