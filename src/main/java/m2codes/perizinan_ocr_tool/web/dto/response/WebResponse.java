package m2codes.perizinan_ocr_tool.web.dto.response;

import lombok.*;

/**
 *
 * @author marij_mokoginta
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebResponse<T> {

    private T data;

    private boolean success;

    private String errorMessage;

}
