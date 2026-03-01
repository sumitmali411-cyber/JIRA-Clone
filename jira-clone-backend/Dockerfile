FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run: mvn package -DskipTests  before building this image
COPY target/jira-clone-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
