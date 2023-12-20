# Hosh

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![CI](https://github.com/dfa1/hosh/workflows/CI/badge.svg)](https://github.com/dfa1/hosh/actions?query=workflow%3ACI) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hosh%3Ahosh-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=hosh%3Ahosh-parent)
 [![`Coverage`](https://sonarcloud.io/api/project_badges/measure?project=hosh%3Ahosh-parent&metric=coverage)](https://sonarcloud.io/dashboard?id=hosh%3Ahosh-parent)[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4646/badge)](https://bestpractices.coreinfrastructure.org/projects/4646)

Human Oriented SHell, written in Java.

Website: https://hosh-shell.github.io

## Try it with Docker

`$ docker run -it --rm ghcr.io/hosh-shell/hosh:0.1.4`

## Development

### Requirements

JDK 17+ (tested with JDK 17 and JDK 21).

### Build

Full build with all fitness and acceptance tests:
`$ ./mvnw clean verify`

Quick build (2x faster on my machine):
`$ ./mvnw -Pskip-slow-tests clean verify`

### Run

`$ java -jar main/target/hosh.jar`

### Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar main/target/hosh.jar`

### Logging

Hosh uses `java.util.logging` (to not require additional dependencies). `HOSH_LOG_LEVEL` controls
logging behaviour according to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default, logging is disabled, to enable it:

`$ HOSH_LOG_LEVEL=FINE java -jar main/target/hosh.jar`

Logging events will be persisted in `$HOME/.hosh.log`.

### Docker support

Build a release with docker:

- `$ cat ~/.github/secret_token | docker login https://ghcr.io -u $USERNAME --password-stdin`
- `$ ./mvnw clean verify # uberjar ready at main/target/`
- `$ docker build -t ghcr.io/hosh-shell/hosh:$VERSION .`
- `$ docker push ghcr.io/hosh-shell/hosh:$VERSION`

### Sonar

`./mvnw clean verify sonar:sonar -Psonar -Dsonar.token=MYTOKEN`

## Sponsors

[![JetBrains](https://raw.githubusercontent.com/JetBrains/logos/master/web/jetbrains/jetbrains-variant-2.svg)](https://www.jetbrains.com/?from=hosh)
[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/java/profiler?from=hosh)
[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=hosh%3Ahosh-parent)
