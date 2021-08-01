# Hosh

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![CI](https://github.com/dfa1/hosh/workflows/CI/badge.svg)](https://github.com/dfa1/hosh/actions?query=workflow%3ACI) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=alert_status)](https://sonarcloud.io/dashboard?id=dfa1_hosh)
 [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=coverage)](https://sonarcloud.io/dashboard?id=dfa1_hosh)

Human Oriented SHell, written in Java.

Website: https://hosh-shell.github.io

## Development

### Requirements

JDK 11

### Build

`$ ./mvnw clean verify`

### Run

`$ java -jar main/target/hosh.jar`

### Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar main/target/hosh.jar`

### Logging

Hosh uses `java.util.logging` (to not require additional dependencies). `HOSH_LOG_LEVEL` controls
logging behaviour according to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default logging is disabled, to enable it:

`$ HOSH_LOG_LEVEL=FINE java -jar main/target/hosh.jar`

Logging events will be persisted in `$HOME/.hosh.log`.

### Docker support

Build a release with docker:

- `$ cat ~/.github/hosh_packages.txt | docker login https://docker.pkg.github.com -u $USERNAME --password-stdin`
- `$ ./mvnw clean verify # uberjar ready at main/target/`
- `docker build -t docker.pkg.github.com/hosh-shell/hosh/hosh:$VERSION .`

## Sponsors

[![JetBrains](https://raw.githubusercontent.com/JetBrains/logos/master/web/jetbrains/jetbrains-variant-2.svg)](https://www.jetbrains.com/?from=hosh)
[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/java/profiler?from=hosh)
