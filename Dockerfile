# Сборка и запуск без IDE: тот же JDK 24, что и локальный toolchain.
FROM eclipse-temurin:24-jdk AS build
WORKDIR /src

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew --no-daemon installDist

FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=build /src/build/install/quiet-water /app

EXPOSE 8888

# Скрипт installDist на Unix делает exec java, поэтому PID 1 ловит SIGTERM.
ENTRYPOINT ["/app/bin/quiet-water"]
