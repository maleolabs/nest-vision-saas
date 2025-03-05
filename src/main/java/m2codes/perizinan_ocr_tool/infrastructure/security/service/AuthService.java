package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.Client;
import m2codes.perizinan_ocr_tool.domain.model.Role;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.ClientService;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.AccountDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.UserResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final ClientService clientService;
    private final UserService userService;
    private final AccountVerificationService accountVerificationService;

    @Transactional
    public WebResponse<UserResponse> register(AccountDataRequest request) {
        try {
            if (userService.existsByUsername(request.getUsername())) {
                return WebResponse.error("username already exists", HttpStatus.BAD_REQUEST);
            }

            if (clientService.existsByEmail(request.getEmail())) {
                return WebResponse.error("email already in used", HttpStatus.BAD_REQUEST);
            }

            User user = Optional.ofNullable(userService.save(
                    UserDataRequest.builder()
                            .username(request.getUsername())
                            .password(request.getPassword())
                            .build(),
                    Role.CLIENT))
                    .orElseThrow(() -> new RuntimeException("User registration failed"));

            clientService.save(request, user);

            accountVerificationService.sendVerificationCode(user);

            UserResponse userResponse = UserResponse.fromModel(user);
            return WebResponse.success(userResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("error while registering the user: {}", Arrays.toString(e.getStackTrace()));
            return WebResponse.error("there was an error while registering the user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String authName = authentication.getName();
            Optional<User> userOpt = userService.findByUsername(authName);
            Optional<Client> clientOpt = clientService.findByEmail(authName);

            if (userOpt.isEmpty() && clientOpt.isEmpty()) {
                throw new IllegalStateException("failed get current user");
            }

            return userOpt.orElseGet(() -> clientOpt.get().getUser());
        }
        throw new IllegalStateException("no user is currently logged in");
    }

}