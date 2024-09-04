package m2codes.perizinan_ocr_tool.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.service.TextExtractionService;
import m2codes.perizinan_ocr_tool.infrastructure.dto.ExtractedTextDto;
import m2codes.perizinan_ocr_tool.infrastructure.dto.OcrResultDto;
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
        } catch (MalformedURLException exception) {
            result.setSuccess(false);
            result.setErrorMessage(exception.getMessage());

            log.error(exception.getMessage());
        } catch (IOException | TesseractException exception) {
            result.setSuccess(false);
            result.setErrorMessage(exception.getMessage());

            log.error(exception.getMessage());
        }

        return result;
    }

    @Override
    public List<ExtractedTextDto> extractKeyValueFromText(String text) {
        List<ExtractedTextDto> extractedTexts = new ArrayList<>();
        
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                ExtractedTextDto extractedText = ExtractedTextDto.builder()
                                                .textKey(key)
                                                .textValue(value)
                                                .build();
                extractedTexts.add(extractedText);
            }
        }
        return extractedTexts;
    }

}