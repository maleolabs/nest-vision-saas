package m2codes.ocr_tool.application.service;

import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard murah & deterministik: entry point yang di-inherit bean ter-proxy TIDAK boleh
 * {@code final}. CGLIB tidak bisa meng-override method final, sehingga proxy Objenesis
 * (constructor tidak dipanggil) akan mengeksekusi implementasi base dengan dependency null.
 */
class SpringProxyContractTest {

    @Test
    void textProcessorEntryPointsMustBeOverridable() throws Exception {
        assertNotFinal(TextProcessorService.class,
                "processOcrRequestWithImageUrl", OcrDataRequest.class, String.class);
        assertNotFinal(TextProcessorService.class,
                "processOcrRequestWithUploadedImage", OcrDataRequest.class, String.class);
    }

    @Test
    void extractedTextQueryEntryPointsMustBeOverridable() throws Exception {
        // Saat ini impl-nya belum di-advise (tidak ada @Async/@Transactional/@Cacheable),
        // tetapi kontrak ini menjaga agar penambahan anotasi tersebut tidak diam-diam
        // memunculkan bug Objenesis + method final yang sama.
        assertNotFinal(ExtractedTextQueryService.class, "checkStatus", String.class);
        assertNotFinal(ExtractedTextQueryService.class, "getByRequestId", String.class);
        assertNotFinal(ExtractedTextQueryService.class, "findByRequestId", String.class);
    }

    private static void assertNotFinal(Class<?> type, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = type.getMethod(methodName, parameterTypes);
        assertThat(Modifier.isFinal(method.getModifiers()))
                .as("%s.%s must stay non-final so Spring/CGLIB proxies delegate to the target bean",
                        type.getSimpleName(), methodName)
                .isFalse();
        assertThat(Modifier.isPublic(method.getModifiers()))
                .as("%s.%s must stay public for proxy interception", type.getSimpleName(), methodName)
                .isTrue();
    }
}
