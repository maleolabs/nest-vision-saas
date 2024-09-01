package m2codes.perizinan_ocr_tool.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import m2codes.perizinan_ocr_tool.domain.model.OcrResult;
import m2codes.perizinan_ocr_tool.domain.service.TextExtractionService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author marij_mokoginta
 */
public class TesseractOcrService implements TextExtractionService {

    private final Tesseract tesseract;

    public TesseractOcrService(Tesseract tesseract) {
        this.tesseract = tesseract;
    }

    @Override
    public OcrResult extractTextFromImage(String imageUrl) {
        OcrResult result = new OcrResult();

        try {
            URL url = new URL(imageUrl);

            BufferedImage image = ImageIO.read(url);

            String text = tesseract.doOCR(image);

            result.setSuccess(true);
        } catch (MalformedURLException exception) {

        } catch (IOException | TesseractException exception) {

        }

        return result;
    }

}