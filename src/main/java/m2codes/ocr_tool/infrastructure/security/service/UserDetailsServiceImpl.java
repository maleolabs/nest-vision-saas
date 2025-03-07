package m2codes.ocr_tool.infrastructure.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import m2codes.ocr_tool.domain.model.AccountStatus;
import m2codes.ocr_tool.domain.model.Client;
import m2codes.ocr_tool.domain.model.User;
import m2codes.ocr_tool.domain.service.ClientService;
import m2codes.ocr_tool.domain.service.UserService;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final ClientService clientService;
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userService.findByUsername(username);
        Optional<Client> clientOpt = clientService.findByEmail(username);
        if (userOpt.isEmpty() && clientOpt.isEmpty()) {
            throw new UsernameNotFoundException("username or password wrong");
        }

        User user = getUser(userOpt, clientOpt);

        return UserDetailsImpl.builder()
                .authorities(user.getAuthorities())
                .password(user.getPassword())
                .username(username)
                .build();
    }

    private static User getUser(Optional<User> userOpt, Optional<Client> clientOpt) {
        boolean isUser = userOpt.isPresent();
        User user = isUser ? userOpt.get() : clientOpt.get().getUser();

        if (user.getStatus().equals(AccountStatus.LOCKED)) {
            throw new LockedException("User account is locked");
        }

        if (user.getStatus().equals(AccountStatus.DISABLED)) {
            throw new DisabledException("User account is disabled");
        }

        if (user.getStatus().equals(AccountStatus.EXPIRED)) {
            throw new CredentialsExpiredException("User credentials have expired");
        }
        return user;
    }

}