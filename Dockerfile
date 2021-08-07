FROM maven:3.8.1-openjdk-11 as builder
WORKDIR /build

COPY ./pom.xml ./
COPY ./runtime/pom.xml ./runtime
COPY ./spi/pom.xml ./spi
COPY ./test-support/pom.xml ./test-support
COPY ./main/pom.xml ./main
COPY ./modules/network/pom.xml ./modules/network
COPY ./modules/terminal/pom.xml ./modules/terminal
COPY ./modules/filesystem/pom.xml ./modules/filesystem
COPY ./modules/system/pom.xml ./modules/system
COPY ./modules/history/pom.xml ./modules/history
COPY ./modules/text/pom.xml ./modules/text
COPY ./spi-test-support/pom.xml ./spi-test-support
RUN find .
RUN mvn --errors --batch-mode dependency:resolve

COPY .git ./.git
COPY CHANGELOG.md .
COPY LICENSE.md .
COPY checkstyle.xml .
COPY pom.xml .
COPY runtime ./runtime
COPY spi ./spi
COPY spi-test-support ./spi-test-support
COPY test-support ./test-support
COPY modules ./modules
COPY runtime ./runtime
COPY main ./main
RUN mvn --errors --batch-mode -DskipTests clean verify

LABEL maintainer="Davide Angelocola <davide.angelocola@gmail.com>"
LABEL org.opencontainers.image.source=https://github.com/hosh-shell/hosh

FROM openjdk:11-jre-slim
COPY --from=builder /build/main/target/hosh.jar /
CMD ["java", "-jar", "/hosh.jar"]

