package m2codes.perizinan_ocr_tool.interfaces.controller;

import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ErrorController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<WebResponse<?>> handleRuntimeException(RuntimeException exception) {
        return ResponseEntity.internalServerError().body(WebResponse.builder().success(false).errorMessage(exception.getMessage()).build());
    }

}