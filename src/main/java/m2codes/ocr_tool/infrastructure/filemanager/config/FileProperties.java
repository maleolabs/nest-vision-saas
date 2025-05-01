package m2codes.ocr_tool.infrastructure.filemanager.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class FileProperties {

    @Value("${file.upload-method}")
    private String uploadMethod;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.external-api-url}")
    private String externalApiUrl;

    @Value("${file.external-api-key}")
    private String externalApiKey;

}