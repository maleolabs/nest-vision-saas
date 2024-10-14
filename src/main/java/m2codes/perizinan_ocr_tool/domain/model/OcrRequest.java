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
@Table(name = "ocr_request")
public class OcrRequest {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="izin_id")
    private Long izinId;

    @Column(name="jenis_perizinan_id")
    private Long jenisPerizinanId;

    @Column(name="syarat_izin_id")
    private Long syaratIzinId;

    @Column(name="image_url")
    private String imageUrl;

    @Column(name="requested_at")
    private Long requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @OneToOne(
            mappedBy="ocrRequest",
            cascade=CascadeType.ALL,
            fetch=FetchType.LAZY
    )
    private OcrResult ocrResults;

}