package m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
 * @author marij_mokoginta
 */
@Configuration
public class PerizinanConfig {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String pmptspApiBaseUrl;

    @Bean
    @Qualifier("perizinanWebClient")
    public WebClient perizinanWebClient() {
        return WebClient.builder()
            .baseUrl(pmptspApiBaseUrl)
            .build();
    }

}