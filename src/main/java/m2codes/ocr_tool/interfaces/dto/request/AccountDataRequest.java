package m2codes.ocr_tool.interfaces.dto.request;

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

    @Size(max = 255, message = "you entered a value that has reached the maximum limit of 255 characters")
    private String fullName;

    @Size(max = 255, message = "you entered a value that has reached the maximum limit of 255 characters")
    private String companyName;

    @NotBlank(message = "phone is required")
    @Size(max = 20, message = "you entered a value that has reached the maximum limit of 20 characters")
    private String phone;

    @Email(message = "invalid email format")
    private String email;

    @Size(max = 255, message = "you entered a value that has reached the maximum limit of 255 characters")
    private String address;

    @Size(max = 255, message = "you entered a value that has reached the maximum limit of 255 characters")
    private String website;

    @Size(max = 255, message = "you entered a value that has reached the maximum limit of 255 characters")
    private String industry;

    @Size(max = 150, message = "you entered a value that has reached the maximum limit of 150 characters")
    private String accountType;

}