FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - smaller image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR from build stage
COPY --from=build /app/target/ride-service-0.0.1-SNAPSHOT.jar app.jar

# Render assigns PORT dynamically
EXPOSE ${PORT:-8077}

# Health check endpoint (add actuator dependency for this)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8077}/actuator/health || exit 1

# CRITICAL: Render uses $PORT environment variable
ENTRYPOINT ["sh", "-c", "java -Xmx512m -jar app.jar --server.port=${PORT:-8077}"]