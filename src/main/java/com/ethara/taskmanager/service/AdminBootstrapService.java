package com.ethara.taskmanager.service;

import com.ethara.taskmanager.config.AdminBootstrapProperties;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties adminBootstrapProperties;

    @Bean
    public ApplicationRunner adminBootstrapRunner() {
        return args -> {
            if (userRepository.existsByRole(Role.ADMIN)) {
                return;
            }

            if (!isFullyConfigured()) {
                log.info("No admin seed configured. Set ADMIN_FULL_NAME, ADMIN_EMAIL, and ADMIN_PASSWORD to bootstrap an admin.");
                return;
            }

            String normalizedEmail = adminBootstrapProperties.email().trim().toLowerCase(Locale.ROOT);

            if (userRepository.existsByEmail(normalizedEmail)) {
                log.warn("Admin bootstrap skipped because a user with email {} already exists.", normalizedEmail);
                return;
            }

            User adminUser = new User();
            adminUser.setFullName(adminBootstrapProperties.fullName().trim());
            adminUser.setEmail(normalizedEmail);
            adminUser.setPassword(passwordEncoder.encode(adminBootstrapProperties.password()));
            adminUser.setRole(Role.ADMIN);

            userRepository.save(adminUser);
            log.info("Initial admin user seeded for {}", normalizedEmail);
        };
    }

    private boolean isFullyConfigured() {
        return StringUtils.hasText(adminBootstrapProperties.fullName())
            && StringUtils.hasText(adminBootstrapProperties.email())
            && StringUtils.hasText(adminBootstrapProperties.password());
    }
}
