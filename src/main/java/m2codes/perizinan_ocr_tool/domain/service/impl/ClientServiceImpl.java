package m2codes.perizinan_ocr_tool.domain.service.impl;

import m2codes.perizinan_ocr_tool.domain.model.Client;
import m2codes.perizinan_ocr_tool.domain.repository.ClientRepository;
import m2codes.perizinan_ocr_tool.domain.service.ClientService;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client save(String email) {
        var client = clientRepository.findFirstByEmail(email).orElse(null);
        if (client != null) {
            return client;
        }
        client = clientRepository.save(
            Client.builder()
                .email(email)
                .build()
        );

        return client;
    }

    @Override
    public Client findByClientId(String clientId) {
        return clientRepository.findFirstByClientId(clientId).orElse(null);
    }

    @Override
    public Client findByEmail(String email) {
        return clientRepository.findFirstByEmail(email).orElse(null);
    }

    @Override
    public void deleteByClientId(String clientId) {
        clientRepository.deleteByClientId(clientId);
    }

}