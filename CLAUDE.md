# Hosh

Human Oriented SHell — an experimental Java shell focused on cross-platform portability, type safety, and usability.

Website: https://hosh-shell.github.io

## Project structure

Multi-module Maven project:

- `spi/` — core abstractions (Command, Channel, Record, ExitStatus)
- `spi-test-support/` — test helpers for SPI
- `test-support/` — JUnit 5 utilities (WithThread, WithExecutor)
- `runtime/` — shell engine (Interpreter, Supervisor, PipelineChannel, Compiler/Parser via ANTLR4)
- `modules/system/` — built-in commands: echo, sleep, withTimeout, withLock, benchmark, …
- `modules/filesystem/` — ls, cd, walk, watch, withLock, …
- `modules/text/` — text processing commands
- `modules/network/` — network interfaces, http client
- `modules/terminal/` — terminal utilities
- `modules/history/` — command history
- `main/` — application entry point (`Hosh.java`), produces the uberjar

## Requirements

JDK 21+

## Build

Full build (unit + integration + fitness + acceptance tests):
```
./mvnw clean verify
```

Quick build (~2× faster, skips slow tests):
```
./mvnw -Pskip-slow-tests clean verify
```

## Run

```
java -jar main/target/hosh.jar
```

## Debug

```
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar main/target/hosh.jar
```

## Logging

Hosh uses `java.util.logging`. Logging is disabled by default. To enable:
```
HOSH_LOG_LEVEL=FINE java -jar main/target/hosh.jar
```

Log events are written to `$HOME/.hosh.log`.

## Other useful commands

Mutation testing:
```
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

Sonar analysis:
```
./mvnw clean verify sonar:sonar -Psonar -Dsonar.token=MYTOKEN
```

## Key architecture notes

- Commands communicate via typed records (not raw strings). Basic types: text, number, path, duration, instant.
- Pipeline stages run concurrently; `Supervisor` manages virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
- Inter-stage data flows through `PipelineChannel` backed by `LinkedTransferQueue`.
- Strict error handling by default — equivalent to `set -euo pipefail` in bash.
- Uses Java Platform Module System (`module-info.java` in every module).

## Testing

- all features are covered by unit tests
- tests are written in JUnit5, mockito and assertj
- all tests have // Given // When // Then sections
- the instance of the class under test is always called "sut" (system under test)
