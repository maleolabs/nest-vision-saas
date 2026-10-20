package m2codes.ocr_tool.interfaces.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateApiKeyRequest(@NotBlank @Size(max = 100) String name) {}
