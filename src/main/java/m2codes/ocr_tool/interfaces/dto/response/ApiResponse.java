package m2codes.ocr_tool.interfaces.dto.response;

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
public class ApiResponse<T> {

    private T data;

    private boolean success;

    private String errorMessage;

    private Integer statusCode;

    public static <T> ApiResponse<T> error(String errorMessage, HttpStatus statusCode) {
        return ApiResponse.<T>builder()
                .data(null)
                .success(false)
                .errorMessage(errorMessage)
                .statusCode(statusCode.value())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, HttpStatus statusCode) {
        return ApiResponse.<T>builder()
                .data(data)
                .success(true)
                .errorMessage(null)
                .statusCode(statusCode.value())
                .build();
    }

}