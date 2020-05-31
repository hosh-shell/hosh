FROM mcr.microsoft.com/java/jre-headless:11-zulu-alpine

LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"

ENTRYPOINT ["java", "-jar", "/usr/local/bin/hosh.jar"]

ARG hosh.jar
ADD main/target/hosh.jar /usr/local/bin/hosh.jar
