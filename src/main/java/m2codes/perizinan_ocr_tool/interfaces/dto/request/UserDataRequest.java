package m2codes.perizinan_ocr_tool.interfaces.dto.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDataRequest {

    private String id;
    private String username;
    private String password;

}