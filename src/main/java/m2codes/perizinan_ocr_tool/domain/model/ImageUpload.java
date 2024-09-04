package m2codes.perizinan_ocr_tool.domain.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Table(name = "image_upload")
public class ImageUpload {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="izin_id", nullable=false)
    private Long izinId;

    @Column(name="jenis_perizinan_id", nullable=false)
    private Long jenisPerizinanId;

    @Column(name="syarat_izin_id", nullable=false)
    private Long syaratIzinId;

    @Column(name="image_url", nullable=false, length=255)
    private String imageUrl;

    @Column(name="uploaded_at", nullable=false)
    private Long uploadedAt;

    @OneToMany(mappedBy="imageUpload", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    private List<OcrResult> ocrResults;

}