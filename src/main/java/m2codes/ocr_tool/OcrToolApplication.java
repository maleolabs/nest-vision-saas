package m2codes.ocr_tool;

import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class OcrToolApplication {

	static {
		// Load bundled OpenCV natives (org.openpnp:opencv) exactly once, before any
		// OCR engine bean method runs (TesseractOcrService/ImageQualityAssessor use
		// Imgcodecs/Imgproc natives, including on @Async executor threads).
		// Static block guarantees this happens before Spring context creation.
		// Note: loadLocally() is the correct API for openpnp 4.5.1-2 on Java >= 12
		// (loadShared() only works on Java <= 11 and falls back with an ERROR log).
		try {
			OpenCV.loadLocally();
			log.info("OpenCV natives loaded successfully");
		} catch (Throwable t) {
			// Do not kill startup: pytesseract path does not need OpenCV and the
			// ensemble guards degrade per-engine. Log loudly so deployment issues surface.
			log.error("Failed to load OpenCV natives - native Tesseract preprocessing will be unavailable", t);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(OcrToolApplication.class, args);
	}

}