package com.lendo.loanapplication.loanapplicationassignment;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for every test that needs a database.
 *
 * <p>Tests run against the same engine and the same Flyway migrations as production, so the schema,
 * the constraints and the locking semantics under test are the real ones. {@code @ServiceConnection}
 * hands the container's JDBC URL, user and password straight to Spring Boot, so no test property file
 * has to know about them.
 */
@ActiveProfiles("test")
public abstract class AbstractPostgresTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
