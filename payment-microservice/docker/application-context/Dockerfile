### Builder for Microservice JAR ###
FROM maven:3-jdk-8 as builder

WORKDIR /project

COPY ./payment-microservice/ .
RUN mvn package

### Microservice JAR Containerization ###
FROM java:8-alpine

WORKDIR /application

COPY --from=builder /project/target/payment-microservice-0.0.1-SNAPSHOT.jar ./payment-microservice.jar

CMD java -jar payment-microservice.jar