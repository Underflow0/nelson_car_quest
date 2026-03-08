package org.nelson.kidbank.service;

import org.nelson.kidbank.entity.User;
import org.nelson.kidbank.exception.DuplicateUsernameException;
import org.nelson.kidbank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createChildUser(String username, String displayName, String rawPassword, Long parentId) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException();
        }

        User child = new User();
        child.setUsername(username);
        child.setDisplayName(displayName);
        child.setPasswordHash(passwordEncoder.encode(rawPassword));
        child.setRole(User.Role.CHILD);
        child.setParentId(parentId);
        child.setEnabled(true);
        child.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(child);
        AUDIT.info("action=CREATE_CHILD actorUserId={} targetUsername={} outcome=SUCCESS",
                parentId, username);
        return saved;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findChildrenOfParent(Long parentId) {
        return userRepository.findByParentId(parentId);
    }

    @Transactional
    public void resetChildPassword(Long childId, Long parentId, String rawPassword) {
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child user not found."));

        if (!parentId.equals(child.getParentId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this user.");
        }

        child.setPasswordHash(passwordEncoder.encode(rawPassword));
        // Clear any lockout on password reset
        child.setFailedLoginAttempts(0);
        child.setLockoutUntil(null);
        userRepository.save(child);

        AUDIT.info("action=RESET_PASSWORD actorUserId={} targetUserId={} outcome=SUCCESS",
                parentId, childId);
    }
}
