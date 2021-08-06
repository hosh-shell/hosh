FROM maven:3.8.1-openjdk-11 as builder
WORKDIR /build

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

