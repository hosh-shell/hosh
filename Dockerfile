FROM openjdk:11-jre-slim-stretch
LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"

ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/local/bin/hosh.jar"]

ARG hosh.jar
ADD target/dist/hosh.jar /usr/local/bin/hosh.jar
