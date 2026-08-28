package com.example.collaboration.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DemoDataInitializer(
            UserRepository users,
            PasswordEncoder encoder,
            @Value("${app.demo-data.enabled:false}") boolean enabled
    ) {
        this.users = users;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        seed("admin@collaboration.local", "Admin123!", "Amina Admin", Role.ADMIN);
        seed("manager@collaboration.local", "Manager123!", "Maya Manager", Role.MANAGER);
        seed("user@collaboration.local", "User123!", "Youssef User", Role.USER);
        log.info("Development demo users are available roles=ADMIN,MANAGER,USER");
    }

    private void seed(String email, String password, String displayName, Role role) {
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new UserEntity(email, encoder.encode(password), displayName, role));
        }
    }
}

