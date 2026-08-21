package m2codes.ocr_tool.application.service.impl;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * P3.9 Vision LLM scaffold: calls OpenAI/Qwen-VL/Llava to extract structured JSON from image.
 * Disabled by default. Enable via ocr.llm.enabled=true and set api key/url.
 * This is the ultimate fallback for blur where Tesseract/Paddle fail.
 * Uses prompt to extract KTP fields and correct OCR typos.
 */
@Slf4j
@Service
public class LlmVisionExtractorService {

    @Value("${ocr.llm.enabled:false}")
    private boolean enabled;

    @Value("${ocr.llm.api-key:}")
    private String apiKey;

    @Value("${ocr.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ocr.llm.model:gpt-4o-mini}")
    private String model;

    @Value("${ocr.llm.prompt:Extract all visible fields from this Indonesian KTP/document image. Return JSON with keys: provinsi, kabupaten, kota, nik (16 digits), nama, tempat/tgl lahir, jenis kelamin, alamat, rt/rw, kelurahan, kecamatan, agama, status perkawinan, pekerjaan, kewarganegaraan, berlaku hingga. Correct OCR typos (O->0 for NIK). If field not visible, omit. Return only JSON.}")
    private String prompt;

    public boolean isEnabled() { return enabled; }

    /**
     * Extract via LLM. Returns null if disabled or fails.
     * Caller should fallback to regex parser if this returns null.
     */
    public List<ExtractedTextDto> extractFromImageBase64(String base64Image, String mimeType) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.info("LLM extractor disabled or no api-key");
            return null;
        }
        try {
            // Use Spring WebFlux WebClient (already in pom) to call OpenAI-compatible API
            // For now we log and return null - IMPLEMENTATION HOOK
            // Example payload:
            // {
            //   "model": "gpt-4o-mini",
            //   "messages": [{"role":"user","content":[{"type":"text","text":prompt},{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,..."}}]}],
            //   "response_format": {"type":"json_object"}
            // }
            log.info("LLM vision hook called for mime={} b64_len={}", mimeType, base64Image.length());
            // TODO: implement WebClient call, parse JSON to List<ExtractedTextDto>
            // Placeholder: return null to indicate fallback to normal parser
            // When implemented, parse JSON keys via Jackson and map to ExtractedTextDto
            return null;
        } catch (Exception e) {
            log.error("LLM extraction failed", e);
            return null;
        }
    }

    public List<ExtractedTextDto> extractFromBytes(byte[] imageBytes, String mimeType) {
        String b64 = Base64.getEncoder().encodeToString(imageBytes);
        return extractFromImageBase64(b64, mimeType);
    }

    // Helper to convert expected KTP JSON to DTOs
    public static List<ExtractedTextDto> mapJsonToDtos(Map<String,String> json) {
        return json.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> ExtractedTextDto.builder().textKey(e.getKey().toLowerCase()).textValue(e.getValue().trim()).build())
                .toList();
    }
}
