FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/auth0-authenticator.jar app.jar

RUN ./mvnw clean package

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=test"]
