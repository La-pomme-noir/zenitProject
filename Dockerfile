# Etapa de construcción
FROM maven:3.8.6-openjdk-18 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]