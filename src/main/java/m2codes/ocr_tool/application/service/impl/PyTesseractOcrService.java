package m2codes.ocr_tool.application.service.impl;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.infrastructure.lib.PythonOcrExcecutor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

@Service(value = "pyTesseractService")
@RequiredArgsConstructor
public class PyTesseractOcrService implements TextExtractionService {

    private final PythonOcrExcecutor excecutor;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        OcrResultDto ocrResult = new OcrResultDto();

        try {
            BufferedImage image = ImageIO.read(new URL(imageUrl));

            if (image == null) {
                ocrResult.setSuccess(false);
                ocrResult.setErrorMessage("Failed to read image from URL");
                return ocrResult;
            }

            File tempFile = File.createTempFile("ocr_input_", ".png");
            ImageIO.write(image, "png", tempFile);

            long startTime = System.currentTimeMillis();

            String result = excecutor.runOcrScript(tempFile.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;

            ocrResult.setSuccess(true);
            ocrResult.setExtractedText(result);
            ocrResult.setDuration(duration);
        } catch (Exception e) {
            ocrResult.setSuccess(false);
            ocrResult.setErrorMessage(e.getMessage());
        }

        return ocrResult;
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        OcrResultDto ocrResult = new OcrResultDto();

        try {
            long startTime = System.currentTimeMillis();

            String result = excecutor.runOcrScript(file.getAbsolutePath());

            long duration = System.currentTimeMillis() - startTime;

            ocrResult.setExtractedText(result);
            ocrResult.setDuration(duration);
            ocrResult.setSuccess(true);
        } catch (Exception e) {
            ocrResult.setSuccess(false);
            ocrResult.setErrorMessage(e.getMessage());
        }

        return ocrResult;
    }

}
