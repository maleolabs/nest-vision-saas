package m2codes.perizinan_ocr_tool.domain.repository;

import jakarta.transaction.Transactional;
import m2codes.perizinan_ocr_tool.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findFirstByEmail(String email);

    Optional<Client> findFirstByClientId(String clientId);

    @Transactional
    void deleteByClientId(String clientId);

}