package m2codes.ocr_tool.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import m2codes.ocr_tool.interfaces.validator.annotation.PasswordMatch;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@PasswordMatch
public class ChangePasswordRequest {

    @NotBlank(message = "password is required")
    private String oldPassword;

    @NotBlank(message = "new password is required")
    @Size(min = 6, max = 100, message = "password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "password confirmation is required")
    private String confirmPassword;

}