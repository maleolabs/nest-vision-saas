package m2codes.perizinan_ocr_tool.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import m2codes.perizinan_ocr_tool.interfaces.dto.request.ImageUploadRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

/**
 *
 * @author marij_mokoginta
 */
@SpringBootTest
public class OcrProcessingServiceTest {

    @Value("${perizinan-dpmptsp.api.base-url}")
    private String perizinanApiBaseUrl;

    @Autowired
    private OcrProcessingService ocrProcessingService;

    @Test
    public void processOcrTest() {
        String imageUrl = perizinanApiBaseUrl + "/files/MTcyNTQzMzA1MDg1NS10ZXN0LmpwZw==";
        ImageUploadRequest request = ImageUploadRequest.builder()
                                        .izinId(5L)
                                        .jenisPerizinanId(167L)
                                        .syaratIzinId(855L)
                                        .imageUrl(imageUrl)
                                        .build();

        WebResponse response = ocrProcessingService.processOcr(request);
        System.out.println(response.toString());
    }

}