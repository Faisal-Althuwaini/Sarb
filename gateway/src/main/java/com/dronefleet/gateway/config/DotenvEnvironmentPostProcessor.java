package com.dronefleet.gateway.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// Loads gateway/.env (gitignored) into the Spring Environment so JWT_SECRET
// never needs to be exported by hand or committed. Must hold the exact same
// value as auth-service/.env - auth-service signs tokens with it, the
// gateway verifies them with it. Registered via META-INF/spring.factories -
// Boot 4.1 still loads EnvironmentPostProcessor that way, not via .imports.
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        try {
            Map<String, Object> values = new LinkedHashMap<>();
            List<String> lines = Files.readAllLines(envFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                values.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
            environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file", e);
        }
    }
}
