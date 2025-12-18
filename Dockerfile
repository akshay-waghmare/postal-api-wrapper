# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Copy source code
COPY src src

# Download dependencies and build
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B && \
    mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
