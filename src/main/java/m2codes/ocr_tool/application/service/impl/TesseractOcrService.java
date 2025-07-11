package m2codes.ocr_tool.application.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import m2codes.ocr_tool.application.service.TextExtractionService;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.application.dto.OcrResultDto;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author marij_mokoginta
 */
@Service(value = "nativeTesseractService")
@Slf4j
public class TesseractOcrService implements TextExtractionService {

    @Value("${tesseract.datapath}")
    private String tessdataPath;

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        try {
            BufferedImage image = getBufferedImage(imageUrl);
            return processOcr(image, preprocessed);
        } catch (IOException e) {
            return createErrorResult("Failed to load image: " + e.getMessage());
        }
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        try {
            return processOcr(file, preprocessed);
        } catch (IOException e) {
            return createErrorResult("Failed to process file: " + e.getMessage());
        }
    }

    private OcrResultDto processOcr(Object input, boolean preprocessed) throws IOException {
        OcrResultDto result = new OcrResultDto();
        Tesseract tesseract = getTesseractInstance();

        try {
            long startTime = System.currentTimeMillis();

            if (preprocessed) {
                input = preprocessImage(input);
                log.info("Trying to do OCR with preprocess image");
            }

            String text = (input instanceof BufferedImage)
                    ? tesseract.doOCR((BufferedImage) input)
                    : tesseract.doOCR((File) input);
            long duration = System.currentTimeMillis() - startTime;

            result.setExtractedText(text);
            result.setSuccess(true);
            result.setDuration(duration);
        } catch (TesseractException e) {
            return createErrorResult("OCR processing error: " + e.getMessage());
        }
        return result;
    }

    private Tesseract getTesseractInstance() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("ind");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(3);
        return tesseract;
    }

    private Object preprocessImage(Object input) throws IOException {
        Mat image;

        if (input instanceof File) {
            image = Imgcodecs.imread(((File) input).getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
        } else if (input instanceof BufferedImage) {
            image = bufferedImageToMat((BufferedImage) input);
        } else {
            throw new IllegalArgumentException("Unsupported image format");
        }

        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(image, image, new Size(5, 5), 0);
        Imgproc.threshold(image, image, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(image, image, kernel);

        File processedFile = File.createTempFile("preprocessed", ".png");
        Imgcodecs.imwrite(processedFile.getAbsolutePath(), image);

        return processedFile;
    }

    private Mat bufferedImageToMat(BufferedImage bufferedImage) {
        Mat mat = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC3);
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                int rgb = bufferedImage.getRGB(x, y);
                mat.put(y, x, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            }
        }
        return mat;
    }

    private BufferedImage getBufferedImage(String imageUrl) throws IOException {
        return ImageIO.read(new URL(imageUrl));
    }

    private OcrResultDto createErrorResult(String errorMessage) {
        OcrResultDto result = new OcrResultDto();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        log.error(errorMessage);
        return result;
    }

}