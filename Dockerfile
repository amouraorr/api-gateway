FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . /workspace
RUN mvn -B -DskipTests -pl api-gateway -am package
RUN mkdir -p /workspace && cp api-gateway/target/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]