package m2codes.ocr_tool.domain.service;

import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.interfaces.dto.request.AccountDataRequest;

import java.util.Optional;

public interface ClientService {

    Client save(AccountDataRequest request, User user);

    Optional<Client> findByClientId(String clientId);

    Optional<Client> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteByClientId(String clientId);

}