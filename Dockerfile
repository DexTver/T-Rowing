# Многоступенчатая сборка. Сборка на официальном образе Maven (JDK 21), рантайм — JRE 21.
# Используем предустановленный Maven, а не Maven Wrapper: образ eclipse-temurin не содержит
# curl/wget, а wrapper типа only-script без них не может скачать дистрибутив Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY seeding-engine/ seeding-engine/
COPY app/ app/
RUN mvn -B -pl app -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/app/target/t-rowing-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
