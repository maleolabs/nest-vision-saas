package m2codes.ocr_tool.interfaces.controller;

import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@ControllerAdvice
public class ErrorController {

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<ApiResponse<?>> handleRejectedExecutionException(RejectedExecutionException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ApiResponse.error(
                        "The server is busy, please try again later.",
                        HttpStatus.SERVICE_UNAVAILABLE
                )
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException exception) {
        // XR-03/VIS-F04: detail exception TIDAK dikirim ke client (info-leak).
        // Full detail di-log sisi server untuk debugging.
        log.error("Unhandled runtime exception", exception);
        return ResponseEntity.internalServerError().body(
                ApiResponse.builder()
                        .data(null)
                        .success(false)
                        .errorMessage("Internal server error")
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build()
        );
    }

}