# build stage
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# runtime stage
FROM eclipse-temurin:26-jre
WORKDIR /app

RUN useradd --system --no-create-home spring
COPY --from=build /app/target/*.jar app.jar
USER spring

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
