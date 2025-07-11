package m2codes.ocr_tool.interfaces.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import m2codes.ocr_tool.interfaces.validator.annotation.FileSize;
import m2codes.ocr_tool.interfaces.validator.annotation.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
public class OcrDataRequest {

    @FileSize(max = 3 * 1024 * 1024)
    @FileType(allowed = {"image/png", "image/jpeg"})
    private MultipartFile image;

    @Pattern(regexp = "^(http|https)://.*$", message = "URL not valid")
    private String imageUrl;

    private List<String> requiredKeys;

    public boolean isUsingUrl() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    public boolean isUsingFile() {
        return image != null && !image.isEmpty();
    }

    public List<String> getRequiredKeys() {
        return requiredKeys != null ? requiredKeys : List.of();
    }
}