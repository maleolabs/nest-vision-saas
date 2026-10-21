package m2codes.ocr_tool.infrastructure.security.service;

import m2codes.ocr_tool.domain.model.AccountType;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.RequestUsage;
import m2codes.ocr_tool.domain.repository.RequestUsageRepository;
import m2codes.ocr_tool.domain.service.ClientService;
import m2codes.ocr_tool.infrastructure.security.util.QuotaDayKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test kuota (VIS-F13 atomic increment + D9/VIS-F14 day-key), Mockito murni
 * tanpa Spring context/DB. Day-key pakai QuotaDayKey.today() yang sebenarnya
 * (deterministik dalam satu proses test).
 *
 * UNEXECUTED di sandbox (tanpa javac + MySQL). Jalankan `mvn test` di dev env.
 */
@ExtendWith(MockitoExtension.class)
class RequestLimitServiceTest {

    @Mock
    private RequestUsageRepository requestUsageRepository;
    @Mock
    private ClientService clientService;

    private RequestLimitService service;

    private static final String CLIENT_ID = "client-1";
    private static final int DAILY_LIMIT = 20;

    @BeforeEach
    void setUp() {
        service = new RequestLimitService(requestUsageRepository, clientService);
    }

    private void stubClient() {
        AccountType accountType = AccountType.builder().dailyLimit(DAILY_LIMIT).build();
        Client client = Client.builder().accountType(accountType).build();
        when(clientService.findByClientId(CLIENT_ID)).thenReturn(Optional.of(client));
    }

    private RequestUsage usageWithCount(int count) {
        return RequestUsage.builder()
                .clientId(CLIENT_ID)
                .requestCount(count)
                .requestDate(QuotaDayKey.today())
                .build();
    }

    @Test
    void isRequestAllowedTrueWhenBelowLimit() {
        stubClient();
        when(requestUsageRepository.findByClientIdAndRequestDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usageWithCount(DAILY_LIMIT - 1)));

        assertTrue(service.isRequestAllowed(CLIENT_ID));
    }

    @Test
    void isRequestAllowedFalseWhenAtLimit() {
        stubClient();
        when(requestUsageRepository.findByClientIdAndRequestDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usageWithCount(DAILY_LIMIT)));

        assertFalse(service.isRequestAllowed(CLIENT_ID));
    }

    @Test
    void isRequestAllowedFalseWhenClientUnknown() {
        when(clientService.findByClientId(anyString())).thenReturn(Optional.empty());
        assertFalse(service.isRequestAllowed(CLIENT_ID));
    }

    @Test
    void getUsedCountTodayReturnsRowCounter() {
        when(requestUsageRepository.findByClientIdAndRequestDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.of(usageWithCount(7)));

        assertEquals(7, service.getUsedCountToday(CLIENT_ID));
    }

    @Test
    void getUsedCountTodayDefaultsToZeroWithoutRow() {
        when(requestUsageRepository.findByClientIdAndRequestDate(anyString(), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertEquals(0, service.getUsedCountToday(CLIENT_ID));
    }

    @Test
    void incrementUsesConditionalUpdateFirst() {
        stubClient();
        when(requestUsageRepository.incrementRequestCountIfBelowLimit(anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(1); // UPDATE atomik berhasil → allowed, fallback tidak perlu

        service.incrementUsage(CLIENT_ID);

        verify(requestUsageRepository).incrementRequestCountIfBelowLimit(
                eq(CLIENT_ID), eq(QuotaDayKey.today()), eq(DAILY_LIMIT));
        verify(requestUsageRepository, never())
                .createRowIfMissing(anyString(), any(LocalDate.class));
    }

    @Test
    void incrementCreatesMissingRowThenRetriesConditionalUpdate() {
        stubClient();
        when(requestUsageRepository.incrementRequestCountIfBelowLimit(anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(0, 1); // percobaan-1 kegagalan (row belum ada), retry → 1
        when(requestUsageRepository.createRowIfMissing(anyString(), any(LocalDate.class)))
                .thenReturn(1); // INSERT IGNORE hari pertama

        service.incrementUsage(CLIENT_ID);

        // day-key harus QuotaDayKey.today() (WIB) — satu definisi "hari" bersama dashboard
        verify(requestUsageRepository).createRowIfMissing(eq(CLIENT_ID), eq(QuotaDayKey.today()));
        verify(requestUsageRepository, times(2))
                .incrementRequestCountIfBelowLimit(eq(CLIENT_ID), eq(QuotaDayKey.today()), eq(DAILY_LIMIT));
    }

    @Test
    void incrementNoOpWhenAlreadyCapped() {
        stubClient();
        when(requestUsageRepository.incrementRequestCountIfBelowLimit(anyString(), any(LocalDate.class), anyInt()))
                .thenReturn(0, 0); // UPDATE pertama & setelah retry tetap 0 → sudah di cap
        when(requestUsageRepository.createRowIfMissing(anyString(), any(LocalDate.class)))
                .thenReturn(0); // row sudah ada → IGNORE

        service.incrementUsage(CLIENT_ID);

        // satu-satunya jalur native itu saja yang dipanggil — tidak ada write lain
        verify(requestUsageRepository, never()).findByClientIdAndRequestDate(anyString(), any(LocalDate.class));
    }

    @Test
    void incrementSkipsEverythingWhenClientUnknown() {
        when(clientService.findByClientId(anyString())).thenReturn(Optional.empty());

        service.incrementUsage(CLIENT_ID);

        verify(requestUsageRepository, never())
                .incrementRequestCountIfBelowLimit(anyString(), any(LocalDate.class), anyInt());
        verify(requestUsageRepository, never())
                .createRowIfMissing(anyString(), any(LocalDate.class));
    }
}
