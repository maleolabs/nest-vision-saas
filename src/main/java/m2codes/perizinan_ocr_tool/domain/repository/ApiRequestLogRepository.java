package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.ApiRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {

    Page<ApiRequestLog> findAllByClientIdOrderByRequestTimeDesc(Pageable pageable, String clientId);

    @Query("""
            SELECT DATE_FORMAT(a.requestTime, '%Y-%m') AS period, COUNT(a)
            FROM ApiRequestLog a
            WHERE a.clientId = :clientId
            GROUP BY period
            ORDER BY period DESC
            """)
    List<Object[]> countRequestsGroupedByMonth(@Param("clientId") String clientId);

    @Query("""
            SELECT DATE(a.requestTime) AS period, COUNT(a)
            FROM ApiRequestLog a
            WHERE a.clientId = :clientId
            GROUP BY period
            ORDER BY period DESC
            """)
    List<Object[]> countRequestsGroupedByDay(@Param("clientId") String clientId);

    Optional<ApiRequestLog> findFirstByClientIdOrderByRequestTimeAsc(String clientId);

}