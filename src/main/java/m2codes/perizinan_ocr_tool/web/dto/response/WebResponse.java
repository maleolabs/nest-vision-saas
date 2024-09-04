package m2codes.perizinan_ocr_tool.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Setter
@Getter
@Builder
public class WebResponse {

    private boolean success;

    private String errorMessage;

}
