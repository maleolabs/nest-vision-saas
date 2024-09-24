package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientTokenResponse {

    private String token;

    private Boolean isValid;

    @Override
    public String toString() {
        return "ClientTokenResponse{" +
                "token=" + token +
                ", isValid=" + isValid +
                "}";
    }
}
