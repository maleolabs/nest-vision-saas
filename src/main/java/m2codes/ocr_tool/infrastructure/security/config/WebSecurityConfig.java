package m2codes.ocr_tool.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import m2codes.ocr_tool.domain.service.UserService;
import m2codes.ocr_tool.infrastructure.security.filter.AccountVerificationFilter;
import m2codes.ocr_tool.infrastructure.security.service.AuthenticationFailureHandlerImpl;
import m2codes.ocr_tool.infrastructure.security.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationFailureHandlerImpl failureHandler;
    private final UserService userService;

    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/dashboard/**", "/auth/**", "/account/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/account/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new AccountVerificationFilter(userService), UsernamePasswordAuthenticationFilter.class)
                .formLogin(login -> login
                        .loginPage("/auth/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                )
                .build();
    }

}