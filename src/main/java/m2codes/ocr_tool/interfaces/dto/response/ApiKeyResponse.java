package m2codes.ocr_tool.interfaces.dto.response;
import java.time.Instant;
public record ApiKeyResponse(Long id, String name, boolean active, Instant createdAt, Instant expiresAt, String secret) {}
