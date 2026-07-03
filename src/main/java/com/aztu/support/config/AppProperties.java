package com.aztu.support.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding of all {@code app.*} configuration. Every business rule
 * (email domain, approval flow, token lifetimes, JWT, SMTP) is configured here.
 */
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private String frontendUrl = "http://localhost:3000";
    private String allowedEmailDomain = "aztu.edu.az";
    private boolean requireEmailVerification = true;

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Mail mail = new Mail();
    private final Tokens tokens = new Tokens();
    private final Storage storage = new Storage();

    @Getter
    @Setter
    public static class Cors {
        /** Comma-separated list of allowed origins. */
        private String allowedOrigins = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private String issuer = "aztu-support";
        private long accessTokenExpirationMinutes = 30;
        private long refreshTokenExpirationDays = 14;
    }

    @Getter
    @Setter
    public static class Mail {
        private String from = "no-reply@aztu.edu.az";
        private String fromName = "AzTU Support";
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Tokens {
        private long passwordResetExpirationMinutes = 60;
        private long emailVerificationExpirationHours = 48;
    }

    @Getter
    @Setter
    public static class Storage {
        private String uploadDir = "./uploads";
    }
}
