# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package

# Stage 2: Runtime with JavaFX
FROM azul/zulu-openjdk:21-jre

# Install JavaFX and X11 dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    wget \
    unzip \
    libgl1-mesa-glx \
    libglu1-mesa \
    libgtk-3-0 \
    xauth \
    && rm -rf /var/lib/apt/lists/*

# Download JavaFX (match version with your pom.xml)
ARG JAVAFX_VERSION=21.0.2
RUN wget -O openjfx.zip https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip && \
    unzip openjfx.zip && \
    mv javafx-sdk-${JAVAFX_VERSION} /opt/javafx && \
    rm openjfx.zip

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar /app/app.jar

# JavaFX module configuration
ENV PATH_TO_FX=/opt/javafx/lib
ENV JAVA_OPTS="--module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml"

# Set DISPLAY environment variable to connect to host's X server
# For Docker Desktop on Windows/macOS, host.docker.internal resolves to the host's IP.
ENV DISPLAY=host.docker.internal:0.0

# Entry point - Removed headless flag
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
