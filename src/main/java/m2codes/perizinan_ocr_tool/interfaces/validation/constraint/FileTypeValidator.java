package m2codes.perizinan_ocr_tool.interfaces.validation.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import m2codes.perizinan_ocr_tool.interfaces.validation.annotation.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class FileTypeValidator implements ConstraintValidator<FileType, MultipartFile> {

    private List<String> allowedTypes;

    @Override
    public void initialize(FileType constraintAnnotation) {
        allowedTypes = Arrays.asList(constraintAnnotation.allowed());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext constraintValidatorContext) {
        if (file == null || allowedTypes.contains(file.getContentType())) {
            return true;
        }

        String allowedFormats = String.join(", ", allowedTypes);

        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(
                "File format not supported. Only allowed: " + allowedFormats
        ).addConstraintViolation();

        return false;
    }
}