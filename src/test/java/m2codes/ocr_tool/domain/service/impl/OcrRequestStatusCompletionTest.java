package m2codes.ocr_tool.domain.service.impl;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regresi durasi UI: updateStatus harus mencatat completedAt pada status terminal
 * (DONE/FAILURE) supaya UI menampilkan total wall-clock, bukan waktu engine OCR saja.
 */
class OcrRequestStatusCompletionTest {

    @Test
    void doneSetsCompletedAt() {
        OcrRequestRepository repo = mock(OcrRequestRepository.class);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(a -> a.getArgument(0));
        OcrRequestServiceImpl service = new OcrRequestServiceImpl(repo);

        OcrRequest req = OcrRequest.builder().requestedAt(Instant.now()).build();
        service.updateStatus(req, RequestStatus.DONE);

        assertEquals(RequestStatus.DONE, req.getStatus());
        assertNotNull(req.getCompletedAt(), "DONE harus mencatat completedAt");
    }

    @Test
    void failureSetsCompletedAt() {
        OcrRequestRepository repo = mock(OcrRequestRepository.class);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(a -> a.getArgument(0));
        OcrRequestServiceImpl service = new OcrRequestServiceImpl(repo);

        OcrRequest req = OcrRequest.builder().requestedAt(Instant.now()).build();
        service.updateStatus(req, RequestStatus.FAILURE);

        assertNotNull(req.getCompletedAt(), "FAILURE juga terminal -> completedAt");
    }

    @Test
    void nonTerminalStatusDoesNotSetCompletedAt() {
        OcrRequestRepository repo = mock(OcrRequestRepository.class);
        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(a -> a.getArgument(0));
        OcrRequestServiceImpl service = new OcrRequestServiceImpl(repo);

        OcrRequest req = OcrRequest.builder().build();
        service.updateStatus(req, RequestStatus.PROCESSING);

        assertNull(req.getCompletedAt(), "PROCESSING bukan terminal -> completedAt null");
    }

    @Test
    void completedAtAfterRequestedAt() {
        OcrRequest req = OcrRequest.builder()
                .requestedAt(Instant.parse("2026-10-31T00:00:00Z"))
                .completedAt(Instant.parse("2026-10-31T00:03:06Z"))
                .build();
        long secs = java.time.Duration.between(req.getRequestedAt(), req.getCompletedAt()).toSeconds();
        assertEquals(186, secs, "wall-clock 3 menit 6 detik");
    }
}
