FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the repackaged fat jar
COPY target/sal-example-adapter-*.jar app.jar

# SAL adapter HTTP port
EXPOSE 8080

# JVM options: container-aware heap, exit on OOM, structured log via logback
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
