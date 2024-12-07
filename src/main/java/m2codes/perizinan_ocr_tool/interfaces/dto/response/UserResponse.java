package m2codes.perizinan_ocr_tool.interfaces.dto.response;

import lombok.Getter;
import lombok.Setter;
import m2codes.perizinan_ocr_tool.domain.model.User;

import java.time.Instant;

@Getter
@Setter
public class UserResponse {

    private String id;

    private String username;

    private Instant lastLogin;

    private Instant createdAt;

    private Instant updatedAt;

    public static UserResponse fromModel(User user) {
        var response = new UserResponse();
        response.setId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setLastLogin(user.getLastLogin());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

}