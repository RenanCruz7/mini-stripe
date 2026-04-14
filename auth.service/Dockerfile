# Multi-stage build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiar arquivos base
COPY mvnw ./
COPY mvnw.cmd ./
COPY pom.xml ./
COPY .mvn ./.mvn

# Copiar source
COPY src ./src

# Build
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -q

# Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/auth.service-*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
