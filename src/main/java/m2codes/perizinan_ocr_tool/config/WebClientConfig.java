package m2codes.perizinan_ocr_tool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
 * @author marij_mokoginta
 */
@Configuration
public class WebClientConfig {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String pmptspApiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
            .baseUrl(pmptspApiBaseUrl)
            .build();
    }

}