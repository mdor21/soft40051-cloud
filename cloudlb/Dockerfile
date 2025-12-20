# Use a lean, production-ready Java 21 JRE image (>= JRE 20 requirement)
FROM eclipse-temurin:21-jre-alpine

# Create app directory inside the container
WORKDIR /opt/app

# Copy the built JAR into the image
COPY target/cloudlb-1.0-SNAPSHOT.jar app.jar

# Expose any port your LB will listen on (adjust later when you add HTTP API)
EXPOSE 8080

# Run the load balancer
ENTRYPOINT ["java", "-jar", "app.jar"]

