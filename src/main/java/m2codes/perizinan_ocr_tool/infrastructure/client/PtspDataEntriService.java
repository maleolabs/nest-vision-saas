package m2codes.perizinan_ocr_tool.infrastructure.client;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import m2codes.perizinan_ocr_tool.client.dto.DataEntriDto;
import m2codes.perizinan_ocr_tool.client.service.DataEntriService;
import reactor.core.publisher.Mono;

/**
 *
 * @author marij_mokoginta
 */
@Service
public class PtspDataEntriService implements DataEntriService {

    private final WebClient webClient;

    public PtspDataEntriService(WebClient webClient) {
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