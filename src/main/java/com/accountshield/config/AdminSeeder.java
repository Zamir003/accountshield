package com.accountshield.config;

import com.accountshield.entity.User;
import com.accountshield.enums.Role;
import com.accountshield.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminSeeder(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.password}") String password,
                       @Value("${app.admin.name}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin seed skipped: an ADMIN user already exists");
            return;
        }

        if (email.isBlank() || password.isEmpty()) {
            log.warn("Admin seed skipped: no ADMIN exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String trimmedName = name.trim();

        if (!normalizedEmail.contains("@")) {
            log.warn("Admin seed skipped: ADMIN_EMAIL is not a valid email");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            log.warn("Admin seed skipped: ADMIN_PASSWORD must be between {} and {} characters",
                    MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH);
            return;
        }
        if (trimmedName.length() < MIN_NAME_LENGTH || trimmedName.length() > MAX_NAME_LENGTH) {
            log.warn("Admin seed skipped: ADMIN_NAME must be between {} and {} characters",
                    MIN_NAME_LENGTH, MAX_NAME_LENGTH);
            return;
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Admin seed skipped: a non-admin user with ADMIN_EMAIL already exists ({}). "
                    + "Promote it via SQL or use a different ADMIN_EMAIL", normalizedEmail);
            return;
        }

        User admin = new User(trimmedName, normalizedEmail, passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Admin seed: initial ADMIN user created ({})", normalizedEmail);
    }
}
