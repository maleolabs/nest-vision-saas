package m2codes.perizinan_ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.model.Client;
import m2codes.perizinan_ocr_tool.domain.model.Role;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.domain.service.ClientService;
import m2codes.perizinan_ocr_tool.domain.service.UserService;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.UserResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    private final ClientService clientService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public WebResponse<UserResponse> register(UserDataRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            return WebResponse.error("username already exists", HttpStatus.BAD_REQUEST);
        }

        try {
            User user = userService.save(request, Role.CLIENT);
            if (user == null) {
                return WebResponse.error("user registration failed", HttpStatus.BAD_REQUEST);
            }
            UserResponse userResponse = UserResponse.fromModel(user);
            return WebResponse.success(userResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("error when registering the user: {}", Arrays.toString(e.getStackTrace()));
            return WebResponse.error("there was an error when registering the user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof String principal) {

            Optional<User> userOpt = userService.findByUsername(principal);
            Optional<Client> clientOpt = clientService.findByEmail(principal);

            if (userOpt.isEmpty() && clientOpt.isEmpty()) {
                throw new IllegalStateException("failed get current user");
            }

            User user = userOpt.orElseGet(() -> clientOpt.get().getUser());
            return UserResponse.fromModel(user);
        }
        throw new IllegalStateException("no user is currently logged in");
    }

}