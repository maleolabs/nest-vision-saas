package m2codes.perizinan_ocr_tool.application.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import m2codes.perizinan_ocr_tool.application.service.TextExtractionService;
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

    private final Tesseract tesseract;

    public TesseractOcrService(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl) {
        OcrResultDto result = new OcrResultDto();

        try {
            URL url = new URL(imageUrl);
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                result.setSuccess(false);
                result.setErrorMessage("image null");
                return result;
            }

            String text = tesseract.doOCR(image);

            result.setExtractedText(text);
            result.setSuccess(true);
            result.setExtractedAt(System.currentTimeMillis());
        } catch (IOException | TesseractException exception) {
            result.setSuccess(false);
            result.setErrorMessage(exception.getMessage());

            log.error(exception.getMessage());
        }

        return result;
    }

}