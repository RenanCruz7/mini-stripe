# Usar a imagem auth-service como base (já tem Java 21)
FROM mini-stripe-auth-service:latest AS runtime

WORKDIR /app

# Remover tudo da imagem anterior
RUN rm -f /app/app.jar

# Copiar apenas o JAR compilado
COPY target/account.service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]

