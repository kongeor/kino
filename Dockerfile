FROM eclipse-temurin:22-jdk

COPY target/kino* /kino/app.jar

CMD ["java", "-jar", "/kino/app.jar"]
