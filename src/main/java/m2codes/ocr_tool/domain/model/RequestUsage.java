package m2codes.ocr_tool.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "request_usages")
public class RequestUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "request_date", nullable = false, updatable = false, columnDefinition = "DATE")
    private Instant requestDate;

    @PrePersist
    protected void onCreate() {
        requestDate = Instant.now();
    }

}