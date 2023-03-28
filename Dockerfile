FROM maven:3.6.3-jdk-8
COPY ./ ./
RUN mvn clean package
EXPOSE 8082
CMD ["java", "-jar", "target/pmage-0.0.1-SNAPSHOT.jar"]