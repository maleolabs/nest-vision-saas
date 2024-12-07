package m2codes.perizinan_ocr_tool.interfaces.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String credential;

    private String password;

}