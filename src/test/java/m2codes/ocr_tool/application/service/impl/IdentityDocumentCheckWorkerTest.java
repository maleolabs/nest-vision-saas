package m2codes.ocr_tool.application.service.impl;

import m2codes.ocr_tool.application.dto.OcrResultDto;
import m2codes.ocr_tool.application.service.TextExtractionService;
import m2codes.ocr_tool.application.util.ExtractedTextCleaner;
import m2codes.ocr_tool.application.util.ExtractedTextMapper;
import m2codes.ocr_tool.domain.model.OcrRequest;
import m2codes.ocr_tool.domain.model.RequestStatus;
import m2codes.ocr_tool.domain.repository.OcrRequestRepository;
import m2codes.ocr_tool.domain.service.ExtractedTextService;
import m2codes.ocr_tool.domain.service.OcrResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdentityDocumentCheckWorkerTest {

    private TextExtractionService ocr;
    private OcrRequestRepository requests;
    private OcrResultService ocrResults;
    private ExtractedTextService extractedTexts;
    private ExtractedTextCleaner cleaner;
    private ExtractedTextMapper mapper;
    private IdentityDocumentCheckWorker worker;

    @BeforeEach
    void setUp() {
        ocr = mock(TextExtractionService.class);
        requests = mock(OcrRequestRepository.class);
        ocrResults = mock(OcrResultService.class);
        extractedTexts = mock(ExtractedTextService.class);
        cleaner = mock(ExtractedTextCleaner.class);
        mapper = mock(ExtractedTextMapper.class);
        worker = new IdentityDocumentCheckWorker(ocr, requests, ocrResults, extractedTexts, cleaner, mapper);
    }

    @Test
    void acceptedKtpPersistsParsedFieldsFromSinglePass() {
        UUID id = UUID.randomUUID();
        OcrRequest req = OcrRequest.builder().id(id).imageUrl("img").status(RequestStatus.PROCESSING).build();
        when(requests.findByIdAndClientId(id, "client")).thenReturn(Optional.of(req));
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcrResultDto ok = OcrResultDto.builder()
                .isSuccess(true).extractedText("PROVINSI GORONTALO\nNIK: 1234567890123456\nNAMA: BUDI\nKTP").build();
        when(ocr.extractTextFromImage(any(File.class), eq(false))).thenReturn(ok);
        when(ocrResults.save(eq(ok), any())).thenReturn(mock(m2codes.ocr_tool.domain.model.OcrResult.class));

        String[] lines = {"PROVINSI GORONTALO", "NIK: 1234567890123456", "NAMA: BUDI", "KTP"};
        when(cleaner.linesCleaner(any())).thenReturn(lines);
        when(mapper.parseLinesByColon(lines)).thenReturn(List.of(
                m2codes.ocr_tool.application.dto.ExtractedTextDto.builder().textKey("nik").textValue("1234567890123456").build()));
        when(mapper.detectAndAddMissingKeyValue(eq(lines), anyList())).thenReturn(List.of());
        when(mapper.filterParsedDataByRequiredKeys(anyList(), anyList())).thenReturn(List.of(
                m2codes.ocr_tool.application.dto.ExtractedTextDto.builder().textKey("nik").textValue("1234567890123456").build()));

        worker.process(id, "client", new File("nonexistent-" + id + ".jpg"), List.of("nik", "nama"));

        verify(extractedTexts).saveAll(anyList(), any());
        assertThat(req.getStatus()).isEqualTo(RequestStatus.DONE);
        assertThat(req.getIdentityDecision()).isEqualTo("ACCEPTED");
        assertThat(req.getIdentityDocumentType()).isEqualTo("KTP");
    }

    @Test
    void ocrFailureRejectsWithoutPersistingResults() {
        UUID id = UUID.randomUUID();
        OcrRequest req = OcrRequest.builder().id(id).imageUrl("img").status(RequestStatus.PROCESSING).build();
        when(requests.findByIdAndClientId(id, "client")).thenReturn(Optional.of(req));
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OcrResultDto fail = OcrResultDto.builder().isSuccess(false).errorMessage("engine error").build();
        when(ocr.extractTextFromImage(any(File.class), eq(false))).thenReturn(fail);

        worker.process(id, "client", new File("nonexistent-" + id + ".jpg"), List.of("nik"));

        verify(ocrResults, never()).save(any(), any());
        assertThat(req.getStatus()).isEqualTo(RequestStatus.FAILURE);
        assertThat(req.getIdentityDecision()).isEqualTo("REJECT");
        assertThat(req.getIdentityReasonCode()).isEqualTo("OCR_FAILED");
    }

    @Test
    void unsupportedDocumentRejected() {
        UUID id = UUID.randomUUID();
        OcrRequest req = OcrRequest.builder().id(id).imageUrl("img").status(RequestStatus.PROCESSING).build();
        when(requests.findByIdAndClientId(id, "client")).thenReturn(Optional.of(req));
        when(requests.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(ocr.extractTextFromImage(any(File.class), eq(false))).thenReturn(
                OcrResultDto.builder().isSuccess(true).extractedText("RANDOM TEXT NO LABELS").build());
        when(ocrResults.save(any(), any())).thenReturn(mock(m2codes.ocr_tool.domain.model.OcrResult.class));
        when(cleaner.linesCleaner(any())).thenReturn(new String[]{"RANDOM TEXT NO LABELS"});
        when(mapper.parseLinesByColon(any())).thenReturn(List.of());
        when(mapper.filterParsedDataByRequiredKeys(anyList(), anyList())).thenReturn(List.of());

        worker.process(id, "client", new File("nonexistent-" + id + ".jpg"), List.of());

        assertThat(req.getIdentityDecision()).isEqualTo("REJECT");
        assertThat(req.getIdentityReasonCode()).isEqualTo("UNSUPPORTED_DOCUMENT");
    }
}
