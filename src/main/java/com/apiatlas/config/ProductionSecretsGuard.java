package com.apiatlas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Refuses to start in the prod profile while any secret still has a placeholder or default value. */
@Component
@Profile("prod")
@Slf4j
public class ProductionSecretsGuard {

    public ProductionSecretsGuard(@Value("${apiatlas.security.jwt.secret}") String jwtSecret,
                                  @Value("${apiatlas.discovery.proxy.ingest-token}") String ingestToken,
                                  @Value("${ATLAS_ADMIN_PASSWORD:admin123}") String adminPassword) {
        check("JWT_SECRET", jwtSecret);
        check("MITM_INGEST_TOKEN", ingestToken);
        check("ATLAS_ADMIN_PASSWORD", adminPassword);
    }

    static void check(String name, String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty() || v.startsWith("change-me") || v.startsWith("replace-me") || v.equals("admin123")) {
            throw new IllegalStateException(name + " is unset or still a default/placeholder value; refusing to start in prod");
        }
    }
}
