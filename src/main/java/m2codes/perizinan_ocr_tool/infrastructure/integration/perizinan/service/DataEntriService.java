package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service;

import java.util.List;

import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import reactor.core.publisher.Mono;

/**
 *
 * @author marij_mokoginta
 */
public interface DataEntriService {

    Mono<List<DataEntriDto>> getByJenisPerizinanId(Long id);

}