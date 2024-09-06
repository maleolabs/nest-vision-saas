package m2codes.perizinan_ocr_tool.interfaces.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedTextResponse {

    private String textKey;

    private String textValue;

}