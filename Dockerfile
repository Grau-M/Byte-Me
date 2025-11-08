# stage 1: build
FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu AS build
WORKDIR /workspace

# install Maven
RUN apt-get update && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src ./src
RUN mvn -B -e -q -DskipTests package

# stage 2: runtime
FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
