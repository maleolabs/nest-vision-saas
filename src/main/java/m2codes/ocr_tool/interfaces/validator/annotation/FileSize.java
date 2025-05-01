package m2codes.ocr_tool.interfaces.validator.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import m2codes.ocr_tool.interfaces.validator.constraint.FileSizeValidator;

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