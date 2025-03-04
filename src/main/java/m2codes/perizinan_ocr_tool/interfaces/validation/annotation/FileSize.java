package m2codes.perizinan_ocr_tool.interfaces.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import m2codes.perizinan_ocr_tool.interfaces.validation.constraint.FileSizeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FileSizeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileSize {
    String message() default "File size exceeds the maximum limit {max} MB";
    long max();

    Class<?>[] groups() default  {};

    Class<? extends Payload>[] payload() default {};
}