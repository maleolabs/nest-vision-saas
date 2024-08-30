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
@Table(name = "image_upload")
public class ImageUpload {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long izin_id;

    @Column(nullable=false)
    private Long syarat_izin_id;

    @Column(nullable=false, length=255)
    private String image_url;

    @Column(nullable=false)
    private Long uploaded_at;

    @OneToMany(mappedBy="image_upload", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    private List<OcrResult> ocr_results;

}