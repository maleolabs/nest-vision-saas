package m2codes.perizinan_ocr_tool.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.interfaces.interceptor.ApiRequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiRequestInterceptor apiRequestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiRequestInterceptor).addPathPatterns("/api/**");
    }
}