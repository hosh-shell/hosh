FROM adoptopenjdk/openjdk11:alpine-jre
LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"

ENTRYPOINT ["java", "-jar", "/usr/local/bin/hosh.jar"]

ARG hosh.jar
ADD target/dist/hosh.jar /usr/local/bin/hosh.jar
