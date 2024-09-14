package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.impl;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.DataEntriEndpoint;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class DataEntriServiceImpl implements DataEntriService {

    private final DataEntriEndpoint dataEntriEndpoint;

    public DataEntriServiceImpl(DataEntriEndpoint dataEntriEndpoint) {
        this.dataEntriEndpoint = dataEntriEndpoint;
    }

    @Async
    @Override
    public CompletableFuture<List<DataEntriDto>> getByJenisPerizinanId(Long id) {
        List<DataEntriDto> dataEntriDtos = Optional.ofNullable(dataEntriEndpoint.getByJenisPerizinanId(id)
                .block()).orElseThrow();
        return CompletableFuture.completedFuture(dataEntriDtos);
    }

}