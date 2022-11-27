FROM openjdk:8-jdk-alpine
ADD /target/*.jar mage-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/mage-0.0.1-SNAPSHOT.jar"]