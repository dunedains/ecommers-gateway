# -- Etapa 1: compilacion --------------------------------------------
FROM ecommers-base AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# -- Etapa 2: runtime ------------------------------------------------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p /logs
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
VOLUME /logs
ENTRYPOINT ["java", "-jar", "app.jar"]
