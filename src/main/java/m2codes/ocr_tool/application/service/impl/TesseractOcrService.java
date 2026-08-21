package m2codes.ocr_tool.application.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ImageQualityAssessor;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
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

    @Value("${ocr.preprocessing.enabled:true}")
    private boolean preprocessingEnabled;

    @Value("${ocr.preprocessing.upscale-threshold:1000}")
    private int upscaleThreshold;

    @Value("${ocr.preprocessing.blur-threshold:100}")
    private double blurThreshold;

    @Value("${ocr.ensemble.fallback-enabled:false}")
    private boolean fallbackEnabled;

    private final ImageQualityAssessor qualityAssessor;

    public TesseractOcrService(ImageQualityAssessor qualityAssessor) {
        this.qualityAssessor = qualityAssessor;
    }

    @Override
    public OcrResultDto extractTextFromImage(String imageUrl, boolean preprocessed) {
        try {
            BufferedImage image = getBufferedImage(imageUrl);
            // auto-quality gate: force preprocess if blurry
            if (!preprocessed && preprocessingEnabled && image != null) {
                Mat gray = bufferedImageToMatGray(image);
                if (gray != null && !gray.empty()) {
                    var q = qualityAssessor.assess(gray);
                    if (q.needsPreprocessing()) {
                        log.info("Auto-enable preprocess: blur={}, brightness={}, contrast={}", q.blurScore, q.brightness, q.contrast);
                        preprocessed = true;
                    }
                    gray.release();
                }
            }
            return processOcr(image, preprocessed);
        } catch (IOException e) {
            return createErrorResult("Failed to load image: " + e.getMessage());
        }
    }

    @Override
    public OcrResultDto extractTextFromImage(File file, boolean preprocessed) {
        try {
            if (!preprocessed && preprocessingEnabled) {
                Mat gray = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                if (gray != null && !gray.empty()) {
                    var q = qualityAssessor.assess(gray);
                    if (q.needsPreprocessing()) {
                        log.info("Auto-enable preprocess for file: blur={}, brightness={}, contrast={}", q.blurScore, q.brightness, q.contrast);
                        preprocessed = true;
                    }
                    gray.release();
                }
            }
            return processOcr(file, preprocessed);
        } catch (IOException e) {
            return createErrorResult("Failed to process file: " + e.getMessage());
        }
    }

    private OcrResultDto processOcr(Object input, boolean preprocessed) throws IOException {
        long startTime = System.currentTimeMillis();

        // if preprocess requested, do it first
        Object ocrInput = input;
        ImageQualityAssessor.QualityResult quality = null;
        if (preprocessed) {
            ocrInput = preprocessImage(input);
            log.info("Trying to do OCR with preprocessed image");
        }

        // try multi-PSM with confidence scoring
        List<Integer> psmCandidates = preprocessed ? List.of(6, 4, 3, 1, 11) : List.of(1, 6, 4);
        OcrResultDto best = null;
        String bestText = null;
        double bestScore = -1;

        for (int psm : psmCandidates) {
            Tesseract tesseract = getTesseractInstance(psm);
            try {
                String text;
                if (ocrInput instanceof BufferedImage) {
                    text = tesseract.doOCR((BufferedImage) ocrInput);
                } else {
                    text = tesseract.doOCR((File) ocrInput);
                }
                double score = scoreText(text);
                // also try NIK whitelist pass for numeric fields is handled in postprocessor
                log.info("PSM {} score {} len {}", psm, score, text != null ? text.length() : 0);
                if (score > bestScore) {
                    bestScore = score;
                    bestText = text;
                    best = buildSuccessResult(text, startTime);
                    best.setConfidence((int) Math.min(95, score));
                    if (quality != null) {
                        best.setBlurScore(quality.blurScore);
                    }
                }
                // early exit if high confidence
                if (score > 85 && text != null && text.length() > 30) break;
            } catch (TesseractException e) {
                log.warn("PSM {} failed: {}", psm, e.getMessage());
            }
        }

        if (best != null && bestText != null && !bestText.isBlank()) {
            best.setDuration(System.currentTimeMillis() - startTime);
            return best;
        }

        // fallback single run if all PSM failed
        Tesseract tesseract = getTesseractInstance(6);
        try {
            String text = (ocrInput instanceof BufferedImage)
                    ? tesseract.doOCR((BufferedImage) ocrInput)
                    : tesseract.doOCR((File) ocrInput);
            OcrResultDto result = buildSuccessResult(text, startTime);
            result.setDuration(System.currentTimeMillis() - startTime);
            result.setConfidence((int) scoreText(text));
            return result;
        } catch (TesseractException e) {
            return createErrorResult("OCR processing error: " + e.getMessage());
        }
    }

    private double scoreText(String text) {
        if (text == null || text.isBlank()) return 0;
        // heuristic: ratio of alphanumeric vs garbage + length bonus + dictionary word ratio
        long alphaNum = text.chars().filter(c -> Character.isLetterOrDigit(c) || c == ':' || c == '/' ).count();
        double ratio = (double) alphaNum / Math.max(1, text.length());
        double lenScore = Math.min(20, text.length() / 5.0);
        // penalize too many single-char lines (noise)
        long noisyLines = text.lines().filter(l -> l.trim().length() <= 2 && !l.isBlank()).count();
        double noisePenalty = noisyLines * 2;
        return ratio * 70 + lenScore - noisePenalty;
    }

    private OcrResultDto buildSuccessResult(String text, long startTime) {
        OcrResultDto r = new OcrResultDto();
        r.setExtractedText(text);
        r.setSuccess(true);
        r.setDuration(System.currentTimeMillis() - startTime);
        return r;
    }

    private Tesseract getTesseractInstance(int psm) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("ind");
        tesseract.setPageSegMode(psm);
        tesseract.setOcrEngineMode(1); // LSTM only, better for blur than 3
        // DPI boost hint
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setVariable("preserve_interword_spaces", "1");
        return tesseract;
    }

    private Tesseract getTesseractInstance() {
        return getTesseractInstance(1);
    }

    private Object preprocessImage(Object input) throws IOException {
        Mat gray;

        if (input instanceof File) {
            gray = Imgcodecs.imread(((File) input).getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
        } else if (input instanceof BufferedImage) {
            gray = bufferedImageToMatGray((BufferedImage) input);
        } else {
            throw new IllegalArgumentException("Unsupported image format");
        }

        if (gray == null || gray.empty()) {
            throw new IOException("Failed to load image for preprocessing");
        }

        // 1. Upscale if small (< threshold) - Tesseract needs 300 DPI
        if (gray.cols() < upscaleThreshold || gray.rows() < upscaleThreshold / 2) {
            double scale = Math.max(2.0, (double) upscaleThreshold / Math.max(gray.cols(), gray.rows()));
            scale = Math.min(scale, 2.5);
            Mat resized = new Mat();
            Imgproc.resize(gray, resized, new Size(gray.cols() * scale, gray.rows() * scale), 0, 0, Imgproc.INTER_CUBIC);
            gray.release();
            gray = resized;
            log.info("Upscaled to {}x{}", gray.cols(), gray.rows());
        }

        // 2. CLAHE for contrast (better than simple OTSU for uneven light)
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        Mat claheDst = new Mat();
        clahe.apply(gray, claheDst);
        gray.release();
        gray = claheDst;

        // 3. Denoise (fastNlMeans) - preserves edges better than Gaussian
        Mat denoised = new Mat();
        Imgproc.fastNlMeansDenoising(gray, denoised, 10, 7, 21);
        gray.release();
        gray = denoised;

        // 4a. Perspective correction (A)
        boolean perspectiveEnabled = !"false".equalsIgnoreCase(System.getenv().getOrDefault("OCR_PERSPECTIVE_ENABLED", "true"));
        if (perspectiveEnabled) {
            Mat warped = perspectiveCorrection(gray);
            if (warped != gray) {
                gray.release();
                gray = warped;
                log.info("Perspective corrected");
            }
        }

        // 4b. Orientation OSD 0/90/180/270 (C)
        boolean osdEnabled = !"false".equalsIgnoreCase(System.getenv().getOrDefault("OCR_OSD_ENABLED", "true"));
        if (osdEnabled) {
            Mat oriented = correctOrientation(gray);
            if (oriented != gray) {
                gray.release();
                gray = oriented;
            }
        }

        // 4c. Deskew Hough 10-45° + small (B)
        Mat deskewed = deskewHough(gray);
        if (deskewed != gray) {
            gray.release();
            gray = deskewed;
        }

        // 5. Assess quality for adaptive threshold choice
        ImageQualityAssessor.QualityResult q = qualityAssessor.assess(gray);
        Mat binary = new Mat();

        // Use adaptive threshold for low contrast/blur, OTSU otherwise
        if (q.isLowContrast || q.isBlurry) {
            Imgproc.adaptiveThreshold(gray, binary, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 10);
            log.info("Using adaptiveThreshold (blur={}, contrast={})", q.blurScore, q.contrast);
        } else {
            // Gaussian blur mild before OTSU
            Mat blurred = new Mat();
            Imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);
            Imgproc.threshold(blurred, binary, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);
            blurred.release();
        }

        // 6. Morph open (remove salt noise) - NOT dilate (which merges chars)
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Mat opened = new Mat();
        Imgproc.morphologyEx(binary, opened, Imgproc.MORPH_OPEN, kernel);
        binary.release();
        kernel.release();

        // 7. Border removal (add white border helps PSM)
        Mat bordered = new Mat();
        Core.copyMakeBorder(opened, bordered, 10, 10, 10, 10, Core.BORDER_CONSTANT, new Scalar(255));
        opened.release();

        gray.release();

        File processedFile = File.createTempFile("preprocessed", ".png");
        Imgcodecs.imwrite(processedFile.getAbsolutePath(), bordered);
        bordered.release();

        log.info("Preprocessed image written to {}", processedFile.getAbsolutePath());
        return processedFile;
    }

    // --- A: Perspective ---
    private Mat perspectiveCorrection(Mat src) {
        try {
            Mat blurred = new Mat();
            Imgproc.GaussianBlur(src, blurred, new Size(5,5), 0);
            Mat edged = new Mat();
            Imgproc.Canny(blurred, 50, 150, 3, false);
            Mat dilated = new Mat();
            Mat k = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
            Imgproc.dilate(edged, dilated, k);
            k.release();
            blurred.release();
            edged.release();
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            dilated.release();
            hierarchy.release();
            if (contours.isEmpty()) return src;
            contours.sort((a,b) -> Double.compare(Imgproc.contourArea(b), Imgproc.contourArea(a)));
            int h = src.rows(), w = src.cols();
            double imgArea = h * (double) w;
            for (int i=0;i<Math.min(5, contours.size());i++) {
                MatOfPoint cnt = contours.get(i);
                double area = Imgproc.contourArea(cnt);
                if (area < imgArea*0.15 || area > imgArea*0.95) continue;
                MatOfPoint approx = new MatOfPoint();
                double peri = Imgproc.arcLength(new org.opencv.core.MatOfPoint2f(cnt.toArray()), true);
                Imgproc.approxPolyDP(new org.opencv.core.MatOfPoint2f(cnt.toArray()), approx, 0.02*peri, true);
                if (approx.rows() == 4) {
                    Point[] pts = approx.toArray();
                    // order tl,tr,br,bl
                    Point[] rect = orderPoints(pts);
                    double widthA = Math.hypot(rect[2].x - rect[3].x, rect[2].y - rect[3].y);
                    double widthB = Math.hypot(rect[1].x - rect[0].x, rect[1].y - rect[0].y);
                    double maxW = Math.max(widthA, widthB);
                    double heightA = Math.hypot(rect[1].x - rect[2].x, rect[1].y - rect[2].y);
                    double heightB = Math.hypot(rect[0].x - rect[3].x, rect[0].y - rect[3].y);
                    double maxH = Math.max(heightA, heightB);
                    maxW = Math.max(maxW, 600); maxH = Math.max(maxH, 400);
                    maxW = Math.min(maxW, w*2); maxH = Math.min(maxH, h*2);
                    Mat srcPts = new Mat(4,1, CvType.CV_32FC2);
                    Mat dstPts = new Mat(4,1, CvType.CV_32FC2);
                    srcPts.put(0,0, rect[0].x, rect[0].y, rect[1].x, rect[1].y, rect[2].x, rect[2].y, rect[3].x, rect[3].y);
                    dstPts.put(0,0, 0,0, maxW-1,0, maxW-1,maxH-1, 0,maxH-1);
                    Mat M = Imgproc.getPerspectiveTransform(srcPts, dstPts);
                    Mat warped = new Mat();
                    Imgproc.warpPerspective(src, warped, M, new Size(maxW, maxH), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, new Scalar(255));
                    srcPts.release(); dstPts.release(); M.release(); approx.release();
                    log.info("Perspective corrected {}x{}", (int)maxW, (int)maxH);
                    return warped;
                }
                approx.release();
            }
            return src;
        } catch (Exception e) {
            log.warn("Perspective failed: {}", e.getMessage());
            return src;
        }
    }

    private Point[] orderPoints(Point[] pts) {
        Point[] rect = new Point[4];
        double minSum = Double.MAX_VALUE, maxSum = -Double.MAX_VALUE;
        double minDiff = Double.MAX_VALUE, maxDiff = -Double.MAX_VALUE;
        for (Point p : pts) {
            double s = p.x + p.y;
            double d = p.x - p.y;
            if (s < minSum) { minSum = s; rect[0]=p; }
            if (s > maxSum) { maxSum = s; rect[2]=p; }
            if (d < minDiff) { minDiff = d; rect[3]=p; }
            if (d > maxDiff) { maxDiff = d; rect[1]=p; }
        }
        // fallback if any null (duplicate)
        for (int i=0;i<4;i++) if (rect[i]==null) rect[i]=pts[i];
        return rect;
    }

    // --- C: Orientation 90/180/270 via brute-force 4 rotations ---
    private Mat correctOrientation(Mat src) {
        try {
            // Try 4 rotations, pick best by trying Tesseract OSD quick score
            // Use downscaled thumb for speed
            int h = src.rows(), w = src.cols();
            if (h==0 || w==0) return src;
            // Quick: if aspect strongly portrait but KTP expected landscape, try 90
            // Brute 4 angles with lightweight Tesseract if available
            // We do heuristic: try small thumb OCR length
            Mat best = src;
            double bestScore = -1;
            int bestAngle = 0;
            // Prepare candidates: 0,90,180,270
            int[] angles = {0,90,180,270};
            for (int ang : angles) {
                Mat cand;
                if (ang==0) cand = src;
                else if (ang==90) { cand = new Mat(); Core.rotate(src, cand, Core.ROTATE_90_CLOCKWISE); }
                else if (ang==180) { cand = new Mat(); Core.rotate(src, cand, Core.ROTATE_180); }
                else { cand = new Mat(); Core.rotate(src, cand, Core.ROTATE_90_COUNTERCLOCKWISE); }
                // Score via small Tesseract run: we use fast heuristic - count horizontal lines via Hough or try OCR if Tess instance available
                double score = 0;
                try {
                    Tesseract t = getTesseractInstance(1);
                    // Write to temp file for scoring
                    File tmp = File.createTempFile("osd_", ".png");
                    Imgcodecs.imwrite(tmp.getAbsolutePath(), cand);
                    String txt = t.doOCR(tmp);
                    tmp.delete();
                    score = scoreText(txt);
                } catch (Exception ex) {
                    // fallback: edge density heuristic
                    Mat b = new Mat(); Imgproc.threshold(cand, b, 0,255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
                    score = Core.countNonZero(b) / (double)(cand.rows()*cand.cols());
                    b.release();
                }
                if (ang!=0 && cand != src) { /* keep for comparison */ }
                if (score > bestScore) { bestScore = score; bestAngle = ang; best = cand; }
                if (ang!=0 && cand != best) cand.release();
            }
            if (bestAngle != 0 && best != src) {
                log.info("OSD corrected angle {} score {}", bestAngle, bestScore);
                return best;
            }
            if (best != src) best.release();
            return src;
        } catch (Exception e) {
            log.warn("OSD failed: {}", e.getMessage());
            return src;
        }
    }

    // --- B: Hough large-angle deskew 10-45 + fallback minAreaRect ---
    private Mat deskewHough(Mat src) {
        try {
            Mat binary = new Mat();
            Imgproc.threshold(src, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
            Mat edges = new Mat();
            Imgproc.Canny(binary, 50, 150, 3, false);
            Mat lines = new Mat();
            Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, 100, src.cols()*0.4, 10);
            Double medianAngle = null;
            if (!lines.empty() && lines.rows() >= 3) {
                List<Double> angles = new ArrayList<>();
                for (int i=0;i<lines.rows();i++) {
                    double[] v = lines.get(i,0);
                    double angle = Math.toDegrees(Math.atan2(v[3]-v[1], v[2]-v[0]));
                    if (angle < -45) angle += 90;
                    if (angle > 45) angle -= 90;
                    if (Math.abs(angle) < 45) angles.add(angle);
                }
                if (angles.size() >= 3) {
                    angles.sort(Double::compare);
                    double median = angles.get(angles.size()/2);
                    double mean = angles.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    double std = Math.sqrt(angles.stream().mapToDouble(a->Math.pow(a-mean,2)).average().orElse(0));
                    if (Math.abs(median) > 0.5 && Math.abs(median) < 45 && std < 10) medianAngle = median;
                }
            }
            lines.release(); edges.release();
            if (medianAngle != null) {
                Mat rotMat = Imgproc.getRotationMatrix2D(new Point(src.cols()/2.0, src.rows()/2.0), medianAngle, 1.0);
                Mat dst = new Mat();
                Imgproc.warpAffine(src, dst, rotMat, src.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, new Scalar(255));
                rotMat.release(); binary.release();
                log.info("Deskew Hough angle {}", medianAngle);
                return dst;
            }
            // fallback minAreaRect for small angles
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            if (!contours.isEmpty()) {
                MatOfPoint biggest = contours.stream().max((a,b)->Double.compare(Imgproc.contourArea(a), Imgproc.contourArea(b))).orElse(null);
                if (biggest != null) {
                    org.opencv.core.RotatedRect rect = Imgproc.minAreaRect(new org.opencv.core.MatOfPoint2f(biggest.toArray()));
                    double angle = rect.angle;
                    if (angle < -45) angle += 90;
                    if (Math.abs(angle) > 0.5 && Math.abs(angle) < 45) {
                        Mat rotMat = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0);
                        Mat dst = new Mat();
                        Imgproc.warpAffine(src, dst, rotMat, src.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE, new Scalar(255));
                        hierarchy.release(); binary.release(); rotMat.release();
                        log.info("Deskew rect angle {}", angle);
                        return dst;
                    }
                }
            }
            hierarchy.release(); binary.release();
            return src;
        } catch (Exception e) {
            log.warn("DeskewHough failed: {}", e.getMessage());
            return src;
        }
    }

    private Mat deskew(Mat src) {
        return deskewHough(src);
    }

    private Mat bufferedImageToMatGray(BufferedImage bi) {
        // proper conversion without double cvtColor
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int rgb = bi.getRGB(x, y);
                mat.put(y, x, new byte[]{(byte)((rgb >> 16) & 0xFF), (byte)((rgb >> 8) & 0xFF), (byte)(rgb & 0xFF)});
            }
        }
        Mat gray = new Mat();
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY);
        mat.release();
        return gray;
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