FROM amazoncorretto:25-headless
WORKDIR /app
COPY build/libs/ktor-sample-all.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "backend.jar"]