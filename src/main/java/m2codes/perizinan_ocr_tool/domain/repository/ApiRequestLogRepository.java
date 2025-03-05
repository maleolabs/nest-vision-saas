package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {

    List<ApiRequestLog> findAllByClientIdOrderByRequestTimeDesc(String clientId);

}