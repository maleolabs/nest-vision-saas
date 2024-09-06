package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import m2codes.perizinan_ocr_tool.application.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import reactor.core.publisher.Mono;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class DataEntriServiceImpl implements DataEntriService {

    private final WebClient webClient;

    public DataEntriServiceImpl(@Qualifier("perizinanWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<List<DataEntriDto>> getByJenisPerizinanId(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/data-entri/{id}")
                    .build(id))
                .retrieve()
                .bodyToFlux(DataEntriDto.class)
                .collectList();
    }

}