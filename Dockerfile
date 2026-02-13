FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

COPY /build/libs/service.jar build/

WORKDIR /app/build
EXPOSE 9080
ENTRYPOINT java -jar service.jar
