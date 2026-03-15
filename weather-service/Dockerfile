# Use OpenJDK 21 with a slim base image
FROM eclipse-temurin:21-jre-alpine

# Define a volume for temporary files
VOLUME /tmp

# Expose port 13001 (matches application.yaml)
EXPOSE 13001

# Copy the pre-built JAR from the local target directory
COPY target/weather-*.jar app.jar

# Set environment variables (add any needed for weather-service)
ENV SPRING_PROFILES_ACTIVE=prod

# Set the entry point to run the JAR file
ENTRYPOINT ["java","-jar","/app.jar"]