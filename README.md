# Hosh

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![CI](https://github.com/dfa1/hosh/workflows/CI/badge.svg)](https://github.com/dfa1/hosh/actions?query=workflow%3ACI) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=alert_status)](https://sonarcloud.io/dashboard?id=dfa1_hosh)
 [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=coverage)](https://sonarcloud.io/dashboard?id=dfa1_hosh)


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

### Windows 7 UAC

If any test fails with "java.nio.file.FileSystemException: A required privilege is not held by the client."
then:

- run **secpol.msc**
- go to Security Settings|Local Policies|User Rights Assignment|Create symbolic links
- add your user name.
- restart your session.

(see https://stackoverflow.com/a/24353758)

