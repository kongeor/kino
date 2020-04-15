FROM adoptopenjdk/openjdk12:x86_64-alpine-jdk-12.0.2_10-slim

COPY target/kino.jar /kino/app.jar

CMD ["java", "-jar", "/kino/app.jar"]
