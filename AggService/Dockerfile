# Use an official Maven image to build and run the application
FROM maven:3.9.6-eclipse-temurin-21

# Set the working directory
WORKDIR /app

# Copy the pom.xml file and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the application's source code
COPY src ./src

# Build the application
RUN mvn package

# Expose the port the application runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/aggservice-1.0.0.jar"]
