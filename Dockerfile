FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/ride-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8077

CMD [ "java", "-jar", "app.jar" ]