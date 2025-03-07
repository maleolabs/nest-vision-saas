package m2codes.ocr_tool.domain.repository;

import m2codes.ocr_tool.domain.model.RequestUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RequestUsageRepository extends JpaRepository<RequestUsage, Long> {

    Optional<RequestUsage> findByClientIdAndRequestDate(String clientId, Instant requestDate);

}