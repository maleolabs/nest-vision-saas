package m2codes.ocr_tool.domain.service.impl;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.model.AccountType;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.repository.AccountTypeRepository;
import m2codes.ocr_tool.domain.repository.ClientRepository;
import m2codes.ocr_tool.domain.service.ClientService;
import m2codes.ocr_tool.interfaces.dto.request.AccountDataRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Override
    public Client save(AccountDataRequest request, User user) {
        AccountType accountType = accountTypeRepository.findByName(request.getAccountType()).orElse(null);
        if (accountType == null) {
            return null;
        }

        var client = clientRepository.findFirstByEmail(request.getEmail()).orElse(null);
        if (client != null) {
            client.setFullName(request.getFullName());
            client.setCompanyName(request.getCompanyName());
            client.setPhone(request.getPhone());
            client.setAddress(request.getAddress());
            client.setWebsite(request.getWebsite());
            client.setIndustry(request.getIndustry());
            client.setAccountType(accountType);

            client = clientRepository.save(client);
        } else {
            client = clientRepository.save(
                    Client.builder()
                            .fullName(request.getFullName())
                            .companyName(request.getCompanyName())
                            .phone(request.getPhone())
                            .email(request.getEmail())
                            .address(request.getAddress())
                            .website(request.getWebsite())
                            .industry(request.getIndustry())
                            .accountType(accountType)
                            .user(user)
                            .build()
            );
        }

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
    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }

    @Override
    public void deleteByClientId(String clientId) {
        clientRepository.deleteByClientId(clientId);
    }

}