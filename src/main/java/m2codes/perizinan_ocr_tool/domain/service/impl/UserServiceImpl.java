package m2codes.perizinan_ocr_tool.domain.service.impl;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.repository.UserRepository;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public User save(UserDataRequest request) {
        String encryptedPassword = bCryptPasswordEncoder.encode(request.getPassword());

        var user = userRepository.findById(request.getId()).orElse(null);
        if (user != null) {
            user.setUsername(request.getUsername());
            user.setPassword(encryptedPassword);
            return userRepository.save(user);
        }

        user = userRepository.save(
            User.builder()
                .username(request.getUsername())
                .password(encryptedPassword)
                .build()
        );

        return user;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public void updateLastLoginById(String id) {
        var user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return;
        }

        user.setLastLogin(System.currentTimeMillis());
        userRepository.save(user);
    }

    @Override
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
}