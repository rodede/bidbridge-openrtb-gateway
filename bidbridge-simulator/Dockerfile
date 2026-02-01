FROM eclipse-temurin:25-jdk AS build
# Build stage: compile and repackage the simulator into a runnable JAR.
WORKDIR /workspace
# Install Maven for the build container.
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*
# Copy only POMs first to leverage Docker layer caching for dependencies.
COPY pom.xml ./
COPY bidbridge-simulator/pom.xml bidbridge-simulator/pom.xml
# Download dependencies without running tests.
RUN mvn -q -f bidbridge-simulator/pom.xml -DskipTests dependency:go-offline
# Copy the rest of the source and build the JAR.
COPY . .
RUN mvn -q -f bidbridge-simulator/pom.xml -DskipTests package spring-boot:repackage

FROM eclipse-temurin:25-jre
# Runtime stage: small JRE image with the fat JAR.
WORKDIR /simulator
# Copy the packaged simulator JAR from the build stage.
COPY --from=build /workspace/bidbridge-simulator/target/*.jar /simulator/bidbridge-simulator.jar
# Runtime overrides via env vars.
ENV JAVA_OPTS=""
ENV SERVER_PORT="8081"
EXPOSE 8081
# Start the app, passing server port and optional JVM options.
# Note: dsps.yml is external; mount it and set DSPS_FILE if needed.
# docker run --rm -p 8081:8081 -v "$PWD/dsps.yml:/simulator/dsps.yml:ro" -e DSPS_FILE=/simulator/dsps.yml bidbridge-simulator:local
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$SERVER_PORT -jar /simulator/bidbridge-simulator.jar"]
