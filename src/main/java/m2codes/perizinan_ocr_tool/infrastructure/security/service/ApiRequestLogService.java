package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import m2codes.perizinan_ocr_tool.domain.repository.ApiRequestLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiRequestLogService {

    private final ApiRequestLogRepository apiRequestLogRepository;

    public Page<ApiRequestLog> findAllByClientId(String clientId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        return apiRequestLogRepository.findAllByClientIdOrderByRequestTimeDesc(pageable, clientId);
    }

    public List<?> getRequestCountByClientIdAndEndpoint(String clientId, String endpoint) {
        Instant firstRequestInstant = apiRequestLogRepository.findFirstByClientIdOrderByRequestTimeAsc(clientId)
                .map(ApiRequestLog::getRequestTime)
                .orElse(Instant.now());

        LocalDate firstRequestDate = firstRequestInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate currentDate = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();

        long daysDiff = ChronoUnit.DAYS.between(firstRequestDate, currentDate);
        boolean isMonthly = daysDiff > 30;

        List<Object[]> results = isMonthly
                ? apiRequestLogRepository.countRequestsGroupedByMonth(clientId, endpoint)
                : apiRequestLogRepository.countRequestsGroupedByDay(clientId, endpoint);

        return results.stream()
                .map(row -> Map.of(
                        "period", convertToFormattedString(row[0].toString(), isMonthly),
                        "count", ((Number) row[1]).intValue()
                ))
                .toList();
    }

    private String convertToFormattedString(String period, boolean isMonthly) {
        try {
            var locale = new Locale("id", "ID");
            DateTimeFormatter inputFormatter = isMonthly
                    ? DateTimeFormatter.ofPattern("yyyy-MM")
                    : DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate date = LocalDate.parse(period + (isMonthly ? "-01" : ""), inputFormatter);

            return isMonthly
                    ? date.format(DateTimeFormatter.ofPattern("MMM", locale))
                    : date.format(DateTimeFormatter.ofPattern("dd MMM", locale));
        } catch (Exception e) {
            log.error("failed to convert to formatted string, got error : {}", e.getMessage());
            return period;
        }
    }

}