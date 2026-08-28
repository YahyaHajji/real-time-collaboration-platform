package com.example.collaboration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfigurationDefaultsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void defaultConfigurationRequiresKeysAndDisablesDemoAccounts() {
        contextRunner.run(context -> {
            assertThat(context.getEnvironment().getProperty(
                    "app.security.jwt.require-configured-keys", Boolean.class
            )).isTrue();
            assertThat(context.getEnvironment().getProperty(
                    "app.demo-data.enabled", Boolean.class
            )).isFalse();
        });
    }

    @Test
    void developmentProfileAllowsEphemeralKeysAndDemoAccounts() {
        contextRunner.withPropertyValues("spring.profiles.active=development").run(context -> {
            assertThat(context.getEnvironment().getProperty(
                    "app.security.jwt.require-configured-keys", Boolean.class
            )).isFalse();
            assertThat(context.getEnvironment().getProperty(
                    "app.demo-data.enabled", Boolean.class
            )).isTrue();
        });
    }
}
