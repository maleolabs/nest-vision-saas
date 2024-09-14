package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataEntriDto {

    private Long id;

    private Long jenisPerizinanId;

    private String nama;

}