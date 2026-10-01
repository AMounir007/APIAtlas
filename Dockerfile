# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime (includes Chromium/Firefox/WebKit system deps for Playwright) ----
FROM eclipse-temurin:25-jre-jammy
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
WORKDIR /app
COPY --from=build /workspace/target/api-atlas.jar app.jar
# Install browsers and their OS dependencies as root, then drop privileges.
RUN java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI org.springframework.boot.loader.launch.PropertiesLauncher \
        install --with-deps chromium firefox \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 atlas \
    && chmod -R a+rX /ms-playwright
USER 10001
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
