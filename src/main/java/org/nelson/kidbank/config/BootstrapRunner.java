package org.nelson.kidbank.config;

import org.nelson.kidbank.entity.AppSetting;
import org.nelson.kidbank.entity.User;
import org.nelson.kidbank.repository.AppSettingRepository;
import org.nelson.kidbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);
    private static final String BOOTSTRAP_KEY = "bootstrap_completed";

    @Value("${app.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.parent.username:admin}")
    private String parentUsername;

    @Value("${app.bootstrap.parent.password:changeme123}")
    private String parentPassword;

    @Value("${app.bootstrap.parent.displayName:Parent Admin}")
    private String parentDisplayName;

    private final AppSettingRepository appSettingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapRunner(AppSettingRepository appSettingRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.appSettingRepository = appSettingRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // If already bootstrapped, skip unconditionally
        if (appSettingRepository.findById(BOOTSTRAP_KEY).isPresent()) {
            log.info("Bootstrap already completed — skipping.");
            return;
        }

        if (!bootstrapEnabled) {
            log.info("Bootstrap disabled via configuration — skipping.");
            return;
        }

        // Guard: don't recreate if username already exists
        if (userRepository.existsByUsername(parentUsername)) {
            log.info("Bootstrap: user '{}' already exists — marking bootstrap complete.", parentUsername);
            appSettingRepository.save(new AppSetting(BOOTSTRAP_KEY, "true"));
            return;
        }

        User parent = new User();
        parent.setUsername(parentUsername);
        parent.setDisplayName(parentDisplayName);
        parent.setPasswordHash(passwordEncoder.encode(parentPassword));
        parent.setRole(User.Role.PARENT);
        parent.setEnabled(true);
        parent.setCreatedAt(LocalDateTime.now());

        userRepository.save(parent);
        appSettingRepository.save(new AppSetting(BOOTSTRAP_KEY, "true"));

        // Never log the plaintext password
        log.info("Bootstrap: initial parent user '{}' created successfully.", parentUsername);
    }
}
