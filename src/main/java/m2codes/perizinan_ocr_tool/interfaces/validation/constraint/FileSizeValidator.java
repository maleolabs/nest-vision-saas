package m2codes.perizinan_ocr_tool.interfaces.validation.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import m2codes.perizinan_ocr_tool.interfaces.validation.annotation.FileSize;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {

    private long maxSize;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        maxSize = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext constraintValidatorContext) {
        return file == null || file.getSize() <= maxSize;
    }

}