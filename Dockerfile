# Multi-stage build for optimal image size and security
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy all source files
COPY . .

# Compile all Java files with UTF-8
RUN javac -encoding UTF-8 *.java

# Runtime stage using lightweight JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes and web assets
COPY --from=builder /app/*.class ./
COPY --from=builder /app/index.html ./
COPY --from=builder /app/README.txt ./

# Render dynamically sets PORT env variable at runtime
ENV PORT=8080
EXPOSE 8080

# Run the web application
CMD ["java", "WebApp"]
