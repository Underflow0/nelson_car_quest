package org.nelson.kidbank.config;

import jakarta.servlet.http.*;
import org.nelson.kidbank.entity.User;
import org.nelson.kidbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final UserRepository userRepository;

    public LoginSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String username = authentication.getName();

        // Reset failed login attempts on success
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);

            AUDIT.info("action=LOGIN_SUCCESS actorUsername={} actorUserId={}", username, user.getId());
        });

        // Redirect by role
        boolean isParent = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_PARENT"));

        response.sendRedirect(request.getContextPath() + (isParent ? "/parent/dashboard" : "/child/dashboard"));
    }
}
