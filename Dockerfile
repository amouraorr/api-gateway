FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /work
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY . .
RUN mvn -B -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /work/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
