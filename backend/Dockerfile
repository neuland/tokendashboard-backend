# The jar is the same on every platform, so the builder always runs natively on the build
# machine ($BUILDPLATFORM); only the runtime stage below is built per target platform.
# Without this, a multi-platform build would run Gradle under emulation.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew buildFatJar --no-daemon

RUN cp build/libs/*-all.jar app.jar || cp build/libs/*.jar app.jar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --chown=appuser:appgroup --from=builder /app/app.jar app.jar

USER appuser

EXPOSE 8080

CMD ["java", "-cp", "app.jar", "io.ktor.server.netty.EngineMain"]
