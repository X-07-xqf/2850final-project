FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY ["2850final project/build.gradle.kts", "./"]
COPY ["2850final project/settings.gradle.kts", "./"]
COPY ["2850final project/gradle", "./gradle"]
COPY ["2850final project/src", "./src"]
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
