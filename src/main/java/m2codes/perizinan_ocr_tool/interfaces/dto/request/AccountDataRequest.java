package m2codes.perizinan_ocr_tool.interfaces.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDataRequest {

    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid UUID format"
    )
    private String id;

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be at least 6 characters")
    private String password;

    private String fullName;

    private String companyName;

    private String phone;

    @Email(message = "invalid email format")
    private String email;

    private String address;

    private String website;

    private String industry;

    private String accountTypeId;

}