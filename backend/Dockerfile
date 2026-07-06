# Backend Dockerfile
# 1. Use Maven image with Eclipse Temurin 21 JDK as the build stage
# 2. Copy the pom.xml and download the dependencies to go offline
# 3. Copy the source code and build the application
# 4. Use Eclipse Temurin 21 JRE as the runtime stage, 
# copy the built JAR from the builder stage, expose port 8080, and set the entry point to run the application

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
