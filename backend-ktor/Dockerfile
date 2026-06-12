# Stage 1: Build stage
FROM gradle:8.5-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew :backend-ktor:installDist --no-daemon

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy
RUN mkdir /app
COPY --from=build /home/gradle/src/backend-ktor/build/install/backend-ktor /app
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["./bin/backend-ktor"]
