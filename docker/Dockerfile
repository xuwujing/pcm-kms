FROM openjdk:8-jre-slim
VOLUME /tmp
ARG JAR_FILE=pcm-kms-server/target/pcm-kms-server-0.2.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
RUN mkdir -p /data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=sqlite"]
