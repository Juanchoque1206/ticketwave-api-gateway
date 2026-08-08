FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline -B
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ticketorder-service-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=
ENV DB_URL=jdbc:postgresql://localhost:5432/ticketwave_aw_torder
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=postgres
ENV JWT_SECRET=ChangeThisSecretKeyInProductionToASecureRandomValueOfAtLeast32BytesLong
ENV JWT_EXPIRATION=86400000
ENV ORDER_TTL_MINUTES=15
ENV MONOLITH_BASE_URL=http://localhost:8081

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
