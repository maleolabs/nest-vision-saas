package m2codes.perizinan_ocr_tool.application.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 *
 * @author marij_mokoginta
 */
@Slf4j
@Configuration
public class OcrConfig {

    @Bean
    public Tesseract tesseract() throws IOException {
        Tesseract tesseract = new Tesseract();

        Path tessdataPath = Files.createTempDirectory("tessdata");
        ClassPathResource resource = new ClassPathResource("static/tessdata");
        Files.walk(resource.getFile().toPath())
                        .forEach(source -> {
                            try {
                                Path destination = tessdataPath.resolve(resource.getFile().toPath().relativize(source));
                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                log.error(e.getMessage());
                            }
                        });

        tesseract.setDatapath(tessdataPath.toString());
        tesseract.setLanguage("ind");

        return tesseract;
    }

}