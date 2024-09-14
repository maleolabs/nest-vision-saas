package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.impl;

import java.util.List;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.DataEntriEndpoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.dto.DataEntriDto;
import reactor.core.publisher.Mono;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class DataEntriEndpointImpl implements DataEntriEndpoint {

    private final WebClient webClient;

    public DataEntriEndpointImpl(@Qualifier("perizinanWebClient") WebClient webClient) {
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