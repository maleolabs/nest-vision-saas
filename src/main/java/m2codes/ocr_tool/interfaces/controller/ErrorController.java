package m2codes.ocr_tool.interfaces.controller;

import m2codes.ocr_tool.interfaces.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.concurrent.RejectedExecutionException;

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
        return ResponseEntity.internalServerError().body(
                ApiResponse.builder()
                        .data(exception.getLocalizedMessage())
                        .success(false)
                        .errorMessage(HttpStatus.INTERNAL_SERVER_ERROR.name())
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build()
        );
    }

}