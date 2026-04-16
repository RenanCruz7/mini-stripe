# Use Alpine Linux with Java pre-installed (or any local image)
FROM eclipse-temurin:21-jdk-focal

WORKDIR /app

COPY target/auth.service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
