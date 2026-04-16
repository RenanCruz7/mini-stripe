# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x ./mvnw && ./mvnw package -DskipTests

# Stage 2: Runtime (imagem final menor)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/account.service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

