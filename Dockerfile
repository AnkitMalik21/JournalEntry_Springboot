# ================================
# Stage 1: Build Stage
# ================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven configuration
COPY Journal-Entry/pom.xml .

# Copy source code (matches your src folder)
COPY Journal-Entry/src ./src

# Build the application
# -DskipTests: Skip running tests during build
RUN mvn clean package -DskipTests

# ================================
# Stage 2: Runtime Stage
# ================================
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy only the JAR file from build stage
# This keeps the final image small
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
