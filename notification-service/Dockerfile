# Multi-stage build for Ingestion Service
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /build

# Copy parent POM and module POMs from project root
COPY pom.xml ./pom.xml
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml

# Copy POM for other modules
COPY ingestion-service/pom.xml ./ingestion-service/pom.xml
COPY processing-alert-service/pom.xml ./processing-alert-service/pom.xml
COPY streaming-service/pom.xml ./streaming-service/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl notification-service -am

# Copy common-lib source code
COPY common-lib/src ./common-lib/src

# Copy ingestion-service source code
COPY notification-service/src ./notification-service/src

# Build the application
RUN mvn clean package -pl notification-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Add metadata
LABEL service="notification-service"
LABEL version="1.0.0"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /build/notification-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser:appgroup

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]