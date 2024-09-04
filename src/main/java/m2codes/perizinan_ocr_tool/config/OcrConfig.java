package m2codes.perizinan_ocr_tool.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.sourceforge.tess4j.Tesseract;

/**
 *
 * @author marij_mokoginta
 */
@Configuration
public class OcrConfig {

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\tesseract\\tessdata");
        tesseract.setLanguage("ind");

        return tesseract;
    }

}