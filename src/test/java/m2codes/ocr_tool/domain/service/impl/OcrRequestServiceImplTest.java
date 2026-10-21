package m2codes.ocr_tool.domain.service.impl;

import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * XR-07: unit test logika dedupe OcrRequest scoped per client (tanpa Spring context).
 *
 * NOTE: BELUM DIAKSEKUSI di sandbox (tanpa javac + MySQL). Jalankan via `mvn test` di dev env.
 */
@ExtendWith(MockitoExtension.class)
class OcrRequestServiceImplTest {

    @Mock
    private OcrRequestRepository ocrRequestRepository;

    @Captor
    private ArgumentCaptor<OcrRequest> ocrRequestCaptor;

    private OcrRequestServiceImpl service;

    private static final String IMAGE_URL = "https://web.example/files/ktp.png";
    private static final String CLIENT_A = "client-a-key";
    private static final String CLIENT_B = "client-b-key";

    @BeforeEach
    void setUp() {
        service = new OcrRequestServiceImpl(ocrRequestRepository);
    }

    private OcrDataRequest request() {
        return OcrDataRequest.builder().imageUrl(IMAGE_URL).build();
    }

    @Test
    void saveReusesExistingRowOnlyForTheSameClient() {
        UUID existingId = UUID.randomUUID();
        when(ocrRequestRepository.findFirstByImageUrlAndClientId(IMAGE_URL, CLIENT_A))
                .thenReturn(Optional.of(OcrRequest.builder().id(existingId).imageUrl(IMAGE_URL).build()));
        when(ocrRequestRepository.save(any(OcrRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OcrRequest result = service.save(request(), RequestStatus.PROCESSING, CLIENT_A);

        assertEquals(existingId, result.getId());
        verify(ocrRequestRepository).findFirstByImageUrlAndClientId(IMAGE_URL, CLIENT_A);
    }

    @Test
    void saveSkipsDedupeLookupAndNeverStoresForeignRowIdWhenClientIsBlank() {
        when(ocrRequestRepository.save(any(OcrRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OcrRequest result = service.save(request(), RequestStatus.PROCESSING, "   ");

        verify(ocrRequestRepository, never()).findFirstByImageUrlAndClientId(anyString(), anyString());
        assertNull(result.getId());
    }

    @Test
    void saveDoesNotLookupWhenImageUrlIsNull() {
        when(ocrRequestRepository.save(any(OcrRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OcrDataRequest noUrl = OcrDataRequest.builder().build();
        service.save(noUrl, RequestStatus.PROCESSING, CLIENT_A);

        verify(ocrRequestRepository, never()).findFirstByImageUrlAndClientId(any(), any());
        assertNull(serviceSaveCapture().getId());
    }

    @Test
    void saveCreatesFreshRowForSameImageOfADifferentClient() {
        // row milik client A harus TIDAK dipakai ulang untuk client B
        when(ocrRequestRepository.findFirstByImageUrlAndClientId(IMAGE_URL, CLIENT_B))
                .thenReturn(Optional.empty());
        when(ocrRequestRepository.save(any(OcrRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OcrRequest result = service.save(request(), RequestStatus.PROCESSING, CLIENT_B);

        assertNull(result.getId());
        verify(ocrRequestRepository).findFirstByImageUrlAndClientId(IMAGE_URL, CLIENT_B);
    }

    private OcrRequest serviceSaveCapture() {
        verify(ocrRequestRepository).save(ocrRequestCaptor.capture());
        return ocrRequestCaptor.getValue();
    }
}
