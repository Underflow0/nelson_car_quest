package org.nelson.kidbank.config;

import jakarta.servlet.http.*;
import org.nelson.kidbank.entity.User;
import org.nelson.kidbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String username = request.getParameter("username");

        userRepository.findByUsername(username).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_ATTEMPTS) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                AUDIT.warn("action=ACCOUNT_LOCKED actorUsername={} actorUserId={} reason=TOO_MANY_FAILURES",
                        username, user.getId());
            }

            userRepository.save(user);
            AUDIT.warn("action=LOGIN_FAILURE actorUsername={} attempts={}", username, attempts);
        });

        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
