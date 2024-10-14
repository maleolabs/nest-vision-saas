package m2codes.perizinan_ocr_tool.application.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.dto.OcrResultDto;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author marij_mokoginta
 */
@Service
@Slf4j
public class TesseractOcrService implements TextExtractionService {

    @Value("classpath:static/tessdata")
    private Resource tessdataDirectory;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl) {
        OcrResultDto result = new OcrResultDto();

        try {
            Tesseract tesseract = getTesseractInstance();

            BufferedImage image = getBufferedImage(imageUrl);
            if (image == null) {
                result.setSuccess(false);
                result.setErrorMessage("image null");
                return result;
            }

            long startTime = System.currentTimeMillis();
            String text = tesseract.doOCR(image);
            long endTime = System.currentTimeMillis();

            long duration = endTime - startTime;

            result.setExtractedText(text);
            result.setSuccess(true);
            result.setDuration(duration);
            result.setExtractedAt(endTime);
        } catch (IOException | TesseractException exception) {
            result.setSuccess(false);
            result.setErrorMessage(exception.getMessage());
            log.error(exception.getMessage());
        }

        return result;
    }

    @Override
    public OcrResultDto extractTextFromImage(File file) {
        OcrResultDto result = new OcrResultDto();

        try {
            Tesseract tesseract = getTesseractInstance();

            long startTime = System.currentTimeMillis();
            String text = tesseract.doOCR(file);
            long endTime = System.currentTimeMillis();

            long duration = endTime - startTime;

            result.setExtractedText(text);
            result.setSuccess(true);
            result.setDuration(duration);
            result.setExtractedAt(endTime);
        } catch (IOException | TesseractException exception) {
            result.setSuccess(false);
            result.setErrorMessage(exception.getMessage());
            log.error(exception.getMessage());
        }

        return result;
    }

    private Tesseract getTesseractInstance() throws IOException{
        Tesseract tesseract = new Tesseract();
        String tessdataPath = tessdataDirectory.getFile().getAbsolutePath();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("ind");
        return tesseract;
    }

    private BufferedImage getBufferedImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        return ImageIO.read(url);
    }

}