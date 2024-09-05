package m2codes.perizinan_ocr_tool.domain.model;

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

    @Column(name="image_url", nullable=false)
    private String imageUrl;

    @Column(name="uploaded_at", nullable=false)
    private Long uploadedAt;

    @OneToOne(
            mappedBy="imageUpload",
            cascade=CascadeType.ALL,
            fetch=FetchType.LAZY
    )
    private OcrResult ocrResults;

}