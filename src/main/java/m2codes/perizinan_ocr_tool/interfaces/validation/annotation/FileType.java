package m2codes.perizinan_ocr_tool.interfaces.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import m2codes.perizinan_ocr_tool.interfaces.validation.constraint.FileTypeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FileTypeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileType {
    String message() default "File format not supported. Only allowed: {allowed}";

    String[] allowed();

    Class<?>[] groups() default  {};

    Class<? extends Payload>[] payload() default {};

}