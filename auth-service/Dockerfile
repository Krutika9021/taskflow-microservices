FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY auth-service auth-service
COPY task-service/pom.xml task-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN chmod +x mvnw && ./mvnw -pl auth-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system taskflow && useradd --system --gid taskflow taskflow
COPY --from=build /workspace/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar app.jar
USER taskflow
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
