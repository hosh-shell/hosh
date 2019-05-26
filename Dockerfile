FROM gcr.io/distroless/java:11
LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"

ENTRYPOINT ["java", "-jar", "/usr/local/bin/hosh.jar"]

ARG hosh.jar
ADD target/dist/hosh.jar /usr/local/bin/hosh.jar
