package m2codes.perizinan_ocr_tool.domain.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author marij_mokoginta
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="ocr_result")
public class OcrResult {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="image_upload_id", nullable=false)
    private ImageUpload imageUpload;

    @Column(name="is_success", nullable=false)
    private boolean isSuccess;

    @Column(name="error_message", nullable=false, length=255)
    private String errorMessage;

    @Column(name="extracted_at", nullable=false)
    private Long extractedAt;

    @OneToMany(mappedBy="ocrResult", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    private List<ExtractedText> extractedText;

}