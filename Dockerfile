FROM openjdk:8
WORKDIR /app
COPY target/mage-0.0.1-SNAPSHOT.jar /app
EXPOSE 8082
CMD ["java", "-jar", "mage-0.0.1-SNAPSHOT.jar"]