# Hosh

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![CI](https://github.com/dfa1/hosh/workflows/CI/badge.svg)](https://github.com/dfa1/hosh/actions?query=workflow%3ACI) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hosh-shell_hosh&metric=alert_status)](https://sonarcloud.io/dashboard?id=hosh-shell_hosh)
 [![`Coverage`](https://sonarcloud.io/api/project_badges/measure?project=hosh-shell_hosh&metric=coverage)](https://sonarcloud.io/dashboard?id=hosh-shell_hosh)[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4646/badge)](https://bestpractices.coreinfrastructure.org/projects/4646)

Human Oriented SHell, written in Java.

Website: https://hosh-shell.github.io

## Try it with Docker

`$ docker run -it --rm ghcr.io/hosh-shell/hosh:0.1.4`

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
logging behaviour according to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default, logging is disabled, to enable it:

`$ HOSH_LOG_LEVEL=FINE java -jar main/target/hosh.jar`

Logging events will be persisted in `$HOME/.hosh.log`.

### Docker support

Build a release with docker:

- `$ cat ~/.github/secret_token | docker login https://ghcr.io -u $USERNAME --password-stdin`
- `$ ./mvnw clean verify # uberjar ready at main/target/`
- `$ docker build -t ghcr.io/hosh-shell/hosh:$VERSION .`
- `$ docker push ghcr.io/hosh-shell/hosh:$VERSION`


## Sponsors

[![JetBrains](https://raw.githubusercontent.com/JetBrains/logos/master/web/jetbrains/jetbrains-variant-2.svg)](https://www.jetbrains.com/?from=hosh)
[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/java/profiler?from=hosh)
