package com.oms.config;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests using Testcontainers.
 * <p>
 * Provides automatic lifecycle management for PostgreSQL and Redis containers.
 * All integration tests should extend this class.
 * <p>
 * Features:
 * - PostgreSQL 16 Alpine container
 * - Redis 7 Alpine container
 * - Automatic startup/shutdown via JUnit 5 extension
 * - Shared containers across test methods (faster execution)
 * - Test profile activation
 * <p>
 * <b>Note:</b> These tests require Docker to be running. If Docker is not available,
 * tests will be skipped automatically. Subclasses should use {@code @DisabledIf("isDockerNotAvailable")}
 * to skip tests when Docker is unavailable.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    /** Docker availability flag, set in static initializer. */
    protected static boolean dockerAvailable = true;

    /**
     * PostgreSQL container using official Postgres 16 Alpine image.
     * Exposed port: 5432 (random host port via getMappedPort)
     */
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    /**
     * Redis container using official Redis 7 Alpine image.
     * Exposed port: 6379 (random host port via getMappedPort)
     */
    @Container
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    static {
        dockerAvailable = checkDockerAvailability();

        if (dockerAvailable) {
            try {
                POSTGRES_CONTAINER.start();
                REDIS_CONTAINER.start();
            } catch (Exception e) {
                System.err.println("WARNING: Could not start Testcontainers. Docker may not be available.");
                System.err.println("Integration tests will be skipped. Error: " + e.getMessage());
                dockerAvailable = false;
            }
        }

        if (dockerAvailable && POSTGRES_CONTAINER.isRunning()) {
            System.setProperty("spring.datasource.url",
                    "jdbc:postgresql://localhost:" + POSTGRES_CONTAINER.getMappedPort(5432) + "/testdb");
            System.setProperty("spring.datasource.username", "test");
            System.setProperty("spring.datasource.password", "test");
            System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        }

        if (dockerAvailable && REDIS_CONTAINER.isRunning()) {
            System.setProperty("spring.data.redis.host", "localhost");
            System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
            System.setProperty("spring.data.redis.enabled", "true");
        }

        System.setProperty("spring.h2.console.enabled", "false");

        if (dockerAvailable) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (POSTGRES_CONTAINER.isRunning()) {
                        POSTGRES_CONTAINER.stop();
                    }
                    if (REDIS_CONTAINER.isRunning()) {
                        REDIS_CONTAINER.stop();
                    }
                } catch (Exception e) {
                    System.err.println("WARNING: Could not stop Testcontainers gracefully: " + e.getMessage());
                }
            }));
        }
    }

    /**
     * Checks if Docker is available by attempting to connect to the Docker daemon.
     * Called from static initializer to set the {@link #dockerAvailable} flag.
     *
     * @return true if Docker is available, false otherwise
     */
    private static boolean checkDockerAvailability() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "info", "--format", "{{.ServerVersion}}");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean completed = p.waitFor(3, TimeUnit.SECONDS);
            if (completed && p.exitValue() == 0) {
                return true;
            }
            System.err.println("WARNING: Docker daemon is not available. Integration tests will be skipped.");
            return false;
        } catch (Exception e) {
            System.err.println("WARNING: Could not check Docker availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * JUnit 5 condition method for {@code @DisabledIf("isDockerNotAvailable")}.
     * Subclasses should annotate with {@code @DisabledIf("isDockerNotAvailable")} to
     * skip tests when Docker is not available.
     *
     * @return true if Docker is not available (tests should be disabled)
     */
    public static boolean isDockerNotAvailable() {
        return !dockerAvailable;
    }
}
