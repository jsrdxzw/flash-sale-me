FROM openjdk:17.0.1-slim
VOLUME /tmp
COPY ./start/target/start-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED"
EXPOSE 8090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
