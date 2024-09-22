package m2codes.perizinan_ocr_tool.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.TokenVerificationService;
import m2codes.perizinan_ocr_tool.infrastructure.security.filter.OcrRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenVerificationService tokenVerificationService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authReq -> authReq.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .addFilterBefore(new OcrRequestFilter(tokenVerificationService), BasicAuthenticationFilter.class)
                .build();
    }

}