package m2codes.perizinan_ocr_tool.web.dto.request;

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

    private Long izinId;

    private Long jenisPerizinanId;

    private Long syaratIzinId;

    private String imageUrl;

}