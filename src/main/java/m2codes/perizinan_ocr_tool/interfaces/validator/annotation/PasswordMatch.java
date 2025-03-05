package m2codes.perizinan_ocr_tool.interfaces.validator.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import m2codes.perizinan_ocr_tool.interfaces.validator.constraint.PasswordMatchValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {
    String message() default "password confirmation not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}