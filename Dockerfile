FROM maven:3.9.2-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/target/ride-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8077

ENV PORT=8077
ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
