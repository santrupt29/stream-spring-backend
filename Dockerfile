# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install FFmpeg for HLS transcoding
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Create directories for video processing
RUN mkdir -p /app/videos /app/videos_hsl /tmp/hls

COPY --from=builder /app/target/spring-stream-backend-0.0.1-SNAPSHOT.jar app.jar

# JVM memory limits for small VMs (512MB heap max, 256MB initial)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
