package m2codes.perizinan_ocr_tool.domain.model;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Table(name="ocr_result")
public class OcrResult {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @OneToOne(
            fetch=FetchType.LAZY,
            cascade = CascadeType.ALL
    )
    @JoinColumn(
            name="ocr_request_id",
            referencedColumnName = "id",
            nullable=false
    )
    private OcrRequest ocrRequest;

    @Column(name="is_success", nullable=false)
    private boolean isSuccess;

    @Column(name="error_message")
    private String errorMessage;

    @Column(name = "duration", nullable = false)
    private Long duration;

    @Column(name="extracted_at", nullable=false)
    private Long extractedAt;

    @Column(name = "original_extracted_text", columnDefinition = "TEXT", nullable = false)
    private String originalExtractedText;

    @OneToMany(
            mappedBy="ocrResult",
            cascade=CascadeType.ALL,
            fetch=FetchType.LAZY
    )
    private List<ExtractedText> extractedText;

    @Override
    public String toString() {
        return "OcrResult{" +
                "id=" + id +
                ", ocrRequest=" + ocrRequest +
                ", isSuccess=" + isSuccess +
                ", errorMessage=" + errorMessage +
                ", duration=" + duration +
                ", extractedAt=" + extractedAt +
                "}";
    }
}