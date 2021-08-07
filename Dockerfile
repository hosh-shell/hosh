FROM maven:3.8.1-openjdk-11 as builder
WORKDIR /build

COPY ./pom.xml ./
COPY ./test-support/pom.xml ./test-support/pom.xml
COPY ./spi-test-support/pom.xml ./spi-test-support/pom.xml
COPY ./spi/pom.xml ./spi/pom.xml
COPY ./main/pom.xml ./main/pom.xml
COPY ./modules/network/pom.xml ./modules/network/pom.xml
COPY ./modules/terminal/pom.xml ./modules/terminal/pom.xml
COPY ./modules/filesystem/pom.xml ./modules/filesystem/pom.xml
COPY ./modules/system/pom.xml ./modules/system/pom.xml
COPY ./modules/history/pom.xml ./modules/history/pom.xml
COPY ./modules/text/pom.xml ./modules/text/pom.xml
COPY ./runtime/pom.xml ./runtime/pom.xml
COPY ./checkstyle.xml ./checkstyle.xml
COPY ./.git ./.git
COPY CHANGELOG.md .
COPY LICENSE.md .
RUN mvn --errors --batch-mode verify dependency:resolve

COPY runtime ./runtime
COPY spi ./spi
COPY spi-test-support ./spi-test-support
COPY test-support ./test-support
COPY modules ./modules
COPY runtime ./runtime
COPY main ./main
RUN mvn --errors --batch-mode clean verify

LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"
LABEL org.opencontainers.image.source=https://github.com/hosh-shell/hosh

FROM openjdk:11-jre-slim
COPY --from=builder /build/main/target/hosh.jar /
CMD ["java", "-jar", "/hosh.jar"]

