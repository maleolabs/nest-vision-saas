package m2codes.perizinan_ocr_tool.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
public class ImageUploadRequest {

    @NotNull
    @NotEmpty
    private Long izinId;

    @NotNull
    @NotEmpty
    private Long jenisPerizinanId;

    @NotNull
    @NotEmpty
    private Long syaratIzinId;

    @NotNull
    @NotEmpty
    private String imageUrl;

}