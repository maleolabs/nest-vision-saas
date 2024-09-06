package m2codes.perizinan_ocr_tool.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name="extracted_text")
public class ExtractedText {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="ocr_result_id", nullable=false)
    private OcrResult ocrResult;

    @Column(name="text_key", nullable=false, length=100)
    private String textKey;

    @Column(columnDefinition="TEXT NOT NULL")
    private String textValue;

    @Column(name = "data_entri_id", nullable = false)
    private Long dataEntriId;

}