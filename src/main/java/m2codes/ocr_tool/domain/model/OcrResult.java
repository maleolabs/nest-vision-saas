package m2codes.ocr_tool.domain.model;

import java.util.List;
import java.util.UUID;

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
    @GeneratedValue(strategy=GenerationType.AUTO)
    @Column(name="id")
    private UUID id;

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

    @Column(name = "duration")
    private Long duration;

    @Column(name = "original_extracted_text", columnDefinition = "TEXT")
    private String originalExtractedText;

    // Statistik observability OCR (untuk halaman hasil TryOCR & dashboard).
    // Sebelumnya hanya ditulis ke log aplikasi (baris [OCR] di stderr python)
    // dan hilang setelah request selesai.
    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "blur_score")
    private Double blurScore;

    @Column(name = "brightness")
    private Double brightness;

    @Column(name = "contrast")
    private Double contrast;

    @Column(name = "psm_used")
    private String psmUsed;

    @Column(name = "engine_used")
    private String engineUsed;

    @Column(name = "super_resolution_applied")
    private Boolean superResolutionApplied;

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
                "}";
    }
}