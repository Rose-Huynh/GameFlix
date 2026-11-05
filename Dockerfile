FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system gameflix && useradd --system --gid gameflix gameflix
COPY --from=build --chown=gameflix:gameflix /workspace/target/*.jar app.jar
USER gameflix
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
