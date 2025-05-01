package m2codes.ocr_tool.domain.repository;

import m2codes.ocr_tool.domain.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountTypeRepository extends JpaRepository<AccountType, UUID> {

    Optional<AccountType> findByName(String name);

}