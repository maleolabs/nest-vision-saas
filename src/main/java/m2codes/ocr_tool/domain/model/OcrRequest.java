package m2codes.ocr_tool.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "ocr_request")
public class OcrRequest {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name="id")
    private UUID id;

    @Column(name="image_url")
    private String imageUrl;

    @Column(name="requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @OneToOne(
            mappedBy="ocrRequest",
            cascade=CascadeType.ALL,
            fetch=FetchType.LAZY
    )
    private OcrResult ocrResults;

    @PrePersist
    protected void onCreate() {
        requestedAt = Instant.now();
    }

}