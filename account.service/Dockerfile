# Use imagem base padrão com Java 21 (igual a auth.service)
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app


# Copiar apenas o JAR compilado
COPY target/account.service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

