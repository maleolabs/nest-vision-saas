package m2codes.perizinan_ocr_tool.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Client {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "website")
    private String website;

    @Column(name = "industry")
    private String industry;

    @Column(nullable = false, unique = true, length = 100)
    private String clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_type_id", nullable = false)
    private AccountType accountType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.clientId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

}