package m2codes.ocr_tool.application.util;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ImageQualityAssessor {

    public static class QualityResult {
        public final double blurScore; // Laplacian variance, higher = sharper
        public final double brightness; // mean 0-255
        public final double contrast; // stddev
        public final boolean isBlurry;
        public final boolean isLowContrast;
        public final boolean isTooDarkOrBright;

        public QualityResult(double blurScore, double brightness, double contrast) {
            this.blurScore = blurScore;
            this.brightness = brightness;
            this.contrast = contrast;
            this.isBlurry = blurScore < 100;
            this.isLowContrast = contrast < 30;
            this.isTooDarkOrBright = brightness < 40 || brightness > 220;
        }

        public boolean needsPreprocessing() {
            return isBlurry || isLowContrast || isTooDarkOrBright;
        }

        public boolean needsSuperResolution() {
            return blurScore < 50;
        }
    }

    public QualityResult assess(Mat gray) {
        // blur via Laplacian variance
        Mat laplacian = new Mat();
        Imgproc.Laplacian(gray, laplacian, org.opencv.core.CvType.CV_64F);
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(laplacian, mean, stddev);
        double variance = stddev.get(0,0)[0] * stddev.get(0,0)[0];

        // brightness & contrast via mean/stddev of gray
        MatOfDouble meanGray = new MatOfDouble();
        MatOfDouble stddevGray = new MatOfDouble();
        Core.meanStdDev(gray, meanGray, stddevGray);
        double brightness = meanGray.get(0,0)[0];
        double contrast = stddevGray.get(0,0)[0];

        laplacian.release();

        log.info("Image quality: blur={}, brightness={}, contrast={}", variance, brightness, contrast);
        return new QualityResult(variance, brightness, contrast);
    }

    public QualityResult assessFromBytes(byte[] imageBytes) {
        // fallback simple
        return new QualityResult(100, 120, 40);
    }
}
