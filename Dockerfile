# ==============================================================================
# Multi-stage Dockerfile for Autonomous Research Workspace Backend
# Built on Eclipse Temurin JDK 21
# ==============================================================================

# Stage 1: Build JAR with Maven Wrapper
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Resolve dependencies offline
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build artifact
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal, Secure Runtime Image
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Create a non-privileged system group and user
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash -m appuser

# Create storage directory for exported research artifacts (PDFs, PPTXs)
RUN mkdir -p /app/artifacts && chown -R appuser:appgroup /app

# Copy built JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

# Run as non-root user
USER appuser

# Expose HTTP REST / SSE port
EXPOSE 8080

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0" \
    ARTIFACTS_STORAGE_DIR=/app/artifacts

# Healthcheck endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
