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
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private String clientId;
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "api_key", nullable = true)
    private String apiKey;

    @Column(name = "key_hash", unique = true, length = 64)
    private String keyHash;
    @Column(name = "secret_nonce", length = 24) private String secretNonce;
    @Column(name = "secret_ciphertext", columnDefinition = "TEXT") private String secretCiphertext;
    @Column(name = "secret_key_version") private Integer secretKeyVersion;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
