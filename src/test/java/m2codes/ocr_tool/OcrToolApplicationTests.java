package m2codes.ocr_tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import m2codes.ocr_tool.application.service.OcrProcessingAsyncExecutor;
import m2codes.ocr_tool.application.service.impl.OcrProcessorService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class OcrToolApplicationTests {

	@Autowired
	private OcrProcessingAsyncExecutor asyncExecutor;

	@Autowired
	private OcrProcessorService ocrProcessorService;

	@Test
	void contextLoads() {
		assertNotNull(asyncExecutor);
		assertNotNull(ocrProcessorService);
	}

}
