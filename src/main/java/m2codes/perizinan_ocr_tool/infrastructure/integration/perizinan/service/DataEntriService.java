package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DataEntriService {

    CompletableFuture<List<DataEntriDto>> getByJenisPerizinanId(Long id);

}
