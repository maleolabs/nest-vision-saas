package m2codes.perizinan_ocr_tool.domain.service.impl;

import lombok.RequiredArgsConstructor;
import m2codes.perizinan_ocr_tool.domain.model.AccountStatus;
import m2codes.perizinan_ocr_tool.domain.model.Role;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.repository.UserRepository;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User save(UserDataRequest request, Role role) {
        String encryptedPassword = passwordEncoder.encode(request.getPassword());

        User user;
        if (request.getId() != null) {
            UUID userId = UUID.fromString(request.getId());
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setUsername(request.getUsername());
                user.setPassword(encryptedPassword);
                user.setRole(role);
                return userRepository.save(user);
            }
            return null;
        }

        user = userRepository.save(
            User.builder()
                .username(request.getUsername())
                .password(encryptedPassword)
                .role(role)
                .status(AccountStatus.PENDING)
                .build()
        );

        return user;
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public void updateLastLoginById(String id) {
        UUID userId = UUID.fromString(id);
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void deleteById(String id) {
        userRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public void activateUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public void disableUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setStatus(AccountStatus.DISABLED);
        userRepository.save(user);
    }

    @Override
    public void lockUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setStatus(AccountStatus.LOCKED);
        userRepository.save(user);
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}