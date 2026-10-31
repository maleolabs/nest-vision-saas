package m2codes.ocr_tool.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import m2codes.ocr_tool.domain.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresi: ApiKeyAuthFilter menulis body TEKS pada 401 ("Token is invalid"),
 * padahal klien web (tryocr) mem-parsing respons dengan res.json() ->
 * SyntaxError: Unexpected token 'T'. Penolakan sekarang harus JSON.
 */
class ApiKeyAuthFilterResponseTest {

    private ApiKeyService apiKeyService;
    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        filter = new ApiKeyAuthFilter(apiKeyService);
    }

    @Test
    void invalidKeyReturnsJsonBody() throws Exception {
        when(apiKeyService.verify("bad")).thenReturn(false);

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/ocr/do-ocr");
        req.addHeader("X-API-KEY", "bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("application/json", res.getContentType());
        String body = res.getContentAsString();
        assertTrue(body.contains("Token is invalid"), "body=" + body);
        assertTrue(body.contains("success") && body.contains("false"), "bukan JSON contract: " + body);
        verify(chain, never()).doFilter(req, res);
    }

    @Test
    void missingKeyHeaderReturnsJsonBody() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/ocr/do-ocr");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("application/json", res.getContentType());
        assertTrue(res.getContentAsString().contains("Missing or Invalid Authorization Header"));
    }

    @Test
    void verifyExceptionReturnsJsonBody() throws Exception {
        when(apiKeyService.verify("boom")).thenThrow(new RuntimeException("db down"));

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/ocr/do-ocr");
        req.addHeader("X-API-KEY", "boom");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertEquals("application/json", res.getContentType());
        assertTrue(res.getContentAsString().contains("Token Verification Failed"));
    }

    @Test
    void validKeyPassesThroughUnchanged() throws Exception {
        when(apiKeyService.verify("good")).thenReturn(true);
        when(apiKeyService.clientId("good")).thenReturn("client-1");

        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/ocr/do-ocr");
        req.addHeader("X-API-KEY", "good");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertEquals("client-1", req.getAttribute("clientId"));
        verify(chain).doFilter(req, res);
    }
}
