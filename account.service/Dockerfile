# Use Alpine Linux with Java pre-installed (or any local image)
FROM eclipse-temurin:21-jdk-focal

WORKDIR /app

COPY target/account.service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

