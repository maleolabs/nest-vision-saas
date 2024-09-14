package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint;

import java.util.List;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import reactor.core.publisher.Mono;

/**
 *
 * @author marij_mokoginta
 */
public interface DataEntriEndpoint {

    Mono<List<DataEntriDto>> getByJenisPerizinanId(Long id);

}