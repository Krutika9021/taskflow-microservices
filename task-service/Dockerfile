FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY task-service task-service
COPY auth-service/pom.xml auth-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN chmod +x mvnw && ./mvnw -pl task-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system taskflow && useradd --system --gid taskflow taskflow
COPY --from=build /workspace/task-service/target/task-service-0.0.1-SNAPSHOT.jar app.jar
USER taskflow
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
