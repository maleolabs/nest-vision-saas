package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.Client;
import m2codes.perizinan_ocr_tool.domain.model.User;

import java.util.Optional;

public interface ClientService {

    Client save(String email, User user);

    Optional<Client> findByClientId(String clientId);

    Optional<Client> findByEmail(String email);

    void deleteByClientId(String clientId);

}