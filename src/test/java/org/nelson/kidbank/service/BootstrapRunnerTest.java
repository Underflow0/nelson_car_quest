package org.nelson.kidbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nelson.kidbank.config.BootstrapRunner;
import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BootstrapRunnerTest {

    @Mock AppSettingRepository appSettingRepo;
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks BootstrapRunner bootstrapRunner;

    private void setFields() {
        ReflectionTestUtils.setField(bootstrapRunner, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapRunner, "parentUsername", "admin");
        ReflectionTestUtils.setField(bootstrapRunner, "parentPassword", "changeme123");
        ReflectionTestUtils.setField(bootstrapRunner, "parentDisplayName", "Parent Admin");
    }

    @Test
    void bootstrap_createsParentOnce() throws Exception {
        setFields();
        when(appSettingRepo.findById("bootstrap_completed")).thenReturn(Optional.empty());
        when(userRepo.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$hash");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appSettingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        bootstrapRunner.run(null);

        verify(userRepo).save(argThat(u -> u.getUsername().equals("admin") && u.getRole() == User.Role.PARENT));
        verify(appSettingRepo).save(argThat(s -> s.getKey().equals("bootstrap_completed")));
    }

    @Test
    void bootstrap_skippedIfAlreadyCompleted() throws Exception {
        setFields();
        AppSetting setting = new AppSetting("bootstrap_completed", "true");
        when(appSettingRepo.findById("bootstrap_completed")).thenReturn(Optional.of(setting));

        bootstrapRunner.run(null);

        verify(userRepo, never()).save(any());
    }

    @Test
    void bootstrap_skippedIfUserAlreadyExists() throws Exception {
        setFields();
        when(appSettingRepo.findById("bootstrap_completed")).thenReturn(Optional.empty());
        when(userRepo.existsByUsername("admin")).thenReturn(true);

        bootstrapRunner.run(null);

        verify(userRepo, never()).save(any());
        verify(appSettingRepo).save(any()); // still marks complete
    }
}
