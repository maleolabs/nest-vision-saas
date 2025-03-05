package m2codes.perizinan_ocr_tool.domain.repository;

import m2codes.perizinan_ocr_tool.domain.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountTypeRepository extends JpaRepository<AccountType, UUID> {
}
