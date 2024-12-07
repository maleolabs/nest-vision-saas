package m2codes.perizinan_ocr_tool.interfaces.dto.response;

import lombok.*;
import org.springframework.http.HttpStatus;

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

    private Integer statusCode;

    public static <T> WebResponse<T> error(String errorMessage, HttpStatus statusCode) {
        return WebResponse.<T>builder()
                .data(null)
                .success(false)
                .errorMessage(errorMessage)
                .statusCode(statusCode.value())
                .build();
    }

    public static <T> WebResponse<T> success(T data, HttpStatus statusCode) {
        return WebResponse.<T>builder()
                .data(data)
                .success(true)
                .errorMessage(null)
                .statusCode(statusCode.value())
                .build();
    }

}