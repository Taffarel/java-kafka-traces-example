# Stage 1: Build the application
FROM openjdk:11-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy project files
COPY . /app

# Build the application
RUN mvn clean package

# Stage 2: Create runtime image
FROM openjdk:11-jre-slim

# Set working directory
WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /app/target/price-action-consumer-1.0-SNAPSHOT.jar /app/

# Run the application
CMD ["java", "-jar", "price-action-consumer-1.0-SNAPSHOT.jar"]