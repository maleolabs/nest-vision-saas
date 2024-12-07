package m2codes.perizinan_ocr_tool.domain.service.impl;

import m2codes.perizinan_ocr_tool.domain.model.Client;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.repository.ClientRepository;
import m2codes.perizinan_ocr_tool.domain.service.ClientService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client save(String email, User user) {
        var client = clientRepository.findFirstByEmail(email).orElse(null);
        if (client != null) {
            return client;
        }
        client = clientRepository.save(
            Client.builder()
                .email(email)
                .user(user)
                .build()
        );

        return client;
    }

    @Override
    public Optional<Client> findByClientId(String clientId) {
        return clientRepository.findFirstByClientId(clientId);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return clientRepository.findFirstByEmail(email);
    }

    @Override
    public void deleteByClientId(String clientId) {
        clientRepository.deleteByClientId(clientId);
    }

}