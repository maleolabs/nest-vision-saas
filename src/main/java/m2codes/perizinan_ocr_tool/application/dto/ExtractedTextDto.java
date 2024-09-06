package m2codes.perizinan_ocr_tool.application.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
public class ExtractedTextDto {

    private String textKey;

    private String textValue;

    private Long dataEntriId;

    @Override
    public String toString() {
        return "ExtractedTextDto{" +
                "textKey=" + textKey +
                ", textValue=" + textValue +
                ", dataEntriId=" + dataEntriId +
                "}";
    }
}