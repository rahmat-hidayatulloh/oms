# ============================================================
# Dockerfile - OMS Application
# ============================================================
# Multi-stage build:
#   Stage 1: Build  (maven:3.9-eclipse-temurin-21)
#   Stage 2: Runtime (eclipse-temurin:21-jre)
# ============================================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install curl for mvnw (required to download Maven Wrapper JAR)
RUN apk add --no-cache curl

WORKDIR /build

# Copy Maven wrapper & pom.xml for dependency caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (layer caching)
# This layer is cached unless pom.xml changes
RUN ./mvnw dependency:go-offline -B -q

# Copy source code and config (checkstyle, spotbugs)
COPY src src
COPY config config

# Build the application
# Skip tests to keep image smaller; tests run in CI pipeline
RUN ./mvnw package -DskipTests -B -q

# Extract layers for Spring Boot layered JAR
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Add curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S oms && adduser -S oms -G oms

WORKDIR /app

# Copy layers from builder stage (optimized for caching)
# Order matters: least frequently changed layers first
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Create directory for logs
RUN mkdir -p /app/logs && chown -R oms:oms /app

# Switch to non-root user
USER oms:oms

# Healthcheck configuration
# Use /api/version endpoint (public, no auth required)
# Actuator on separate port 8081 has known Spring Security issues
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/version || exit 1

# Expose application port
EXPOSE 8080

# Default JVM options
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]