package m2codes.ocr_tool.interfaces.validator.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import m2codes.ocr_tool.interfaces.dto.request.ChangePasswordRequest;
import m2codes.ocr_tool.interfaces.validator.annotation.PasswordMatch;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, ChangePasswordRequest> {

    @Override
    public boolean isValid(ChangePasswordRequest request, ConstraintValidatorContext context) {
        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            return false;
        }
        return request.getNewPassword().equals(request.getConfirmPassword());
    }

}