package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.Client;

public interface ClientService {

    Client save(String email);

    Client findByClientId(String clientId);

    Client findByEmail(String email);

    void deleteByClientId(String clientId);

}