# transit-api — multi-stage build.
#   Stage 1 builds the installDist distribution with the Gradle wrapper.
#   Stage 2 runs it on a slim JRE 17. Only the runtime artifact ships,
#   so the final image carries no Gradle/JDK build tooling.

# ---- build stage ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /src

# Warm the Gradle wrapper + dependency cache before copying sources, so
# a source-only change reuses the cached layers.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon installDist

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Run as an unprivileged user, not root.
RUN useradd --system --uid 10001 --no-create-home transit
COPY --from=build /src/build/install/transit-api/ ./

# Heap ceiling matches build.gradle.kts (-Xmx1536m). stop_times.txt is
# streamed, not buffered, so peak usage stays well under this.
ENV JAVA_OPTS="-Xmx1536m"
ENV PORT=8080

USER transit
EXPOSE 8080

# Container-level liveness via bash /dev/tcp — no curl/wget needed in the
# image. Probes the /health route the app serves (Routes.kt).
HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${PORT:-8080}; \
    printf "GET /health HTTP/1.0\r\n\r\n" >&3; \
    grep -q "200 OK" <&3' || exit 1

ENTRYPOINT ["/app/bin/transit-api"]
