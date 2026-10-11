# Multi-stage: construye el jar dentro de Docker (Render no tiene target/ en git)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
# Capa de dependencias (se reutiliza si solo cambia src/)
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline -DskipTests
COPY src ./src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
