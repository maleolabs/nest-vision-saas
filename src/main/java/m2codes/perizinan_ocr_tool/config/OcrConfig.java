package m2codes.perizinan_ocr_tool.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 *
 * @author marij_mokoginta
 */
@Configuration
public class OcrConfig {

    @Value("classpath:static/tessdata")
    private Resource tessdataDirectory;

    @Bean
    public Tesseract tesseract() throws IOException {
        Tesseract tesseract = new Tesseract();

        String tessdataPath = tessdataDirectory.getFile().getAbsolutePath();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("ind");

        return tesseract;
    }

}