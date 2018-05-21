FROM openjdk:8-jdk-alpine
VOLUME /tmp


ENV AGORA_ENDPOINT http://localhost:80

EXPOSE 8081

ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=8081","-jar","/app.jar"]