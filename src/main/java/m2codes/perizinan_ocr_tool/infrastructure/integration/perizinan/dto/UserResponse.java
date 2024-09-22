package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String name;

    private String username;

    private String email;

}