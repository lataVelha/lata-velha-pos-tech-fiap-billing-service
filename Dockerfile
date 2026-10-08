FROM maven:3-amazoncorretto-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine3.20

WORKDIR /app

COPY --from=build /app/target/*.jar ./billing-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "billing-service.jar"]
