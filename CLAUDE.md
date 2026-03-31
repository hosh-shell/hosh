# Hosh

Human Oriented SHell — an experimental Java shell focused on cross-platform portability, type safety, and usability.

Website: <https://hosh-shell.github.io>

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

## Package map

Root package: `hosh`

- `hosh.spi` — all core types: `Record`, `Key`, `Value` (and subtypes: `Text`, `Number`, `Path`, `Duration`, `Instant`), `Command`, `Channel`, `ExitStatus`, `InputChannel`, `OutputChannel`
- `hosh.runtime` — shell engine: `Interpreter`, `Supervisor`, `Compiler`, `Parser` (ANTLR4 grammar in `runtime/src/main/antlr4/`)
- `hosh.runtime.prompt` — prompt rendering
- `hosh.runtime.completion` — tab completion
- `hosh.modules.system` — built-in system commands (echo, env, sleep, exit, benchmark, etc.)
- `hosh.modules.filesystem` — filesystem commands (ls, cd, walk, cp, mv, rm, find, etc.)
- `hosh.modules.text` — text processing (grep, sort, count, split, join, trim, regex, etc.)
- `hosh.modules.network` — network commands (http, resolve, etc.)
- `hosh.modules.terminal` — terminal commands (clear, etc.)
- `hosh.modules.history` — history command

Each module registers its commands via a `Module` SPI implementation (look for `implements Module`).

## Key classes (start here, don't grep)

| Concern | Class | Location |
|---|---|---|
| Entry point | `Hosh` | `main/` |
| Script parsing | `Compiler`, `Parser` | `runtime/` |
| ANTLR4 grammar | `Hosh.g4` | `runtime/src/main/antlr4/` |
| Pipeline execution | `Supervisor` | `runtime/` |
| Inter-stage data flow | `PipelineChannel` | `runtime/` |
| Core record type | `Record`, `Records` | `spi/` |
| Typed values | `Value`, `Keys` | `spi/` |
| Command interface | `Command` | `spi/` |
| Command arguments | `CommandArguments`, `CommandArgument` | `spi/` |
| Channels | `InputChannel`, `OutputChannel` | `spi/` |
| Module registration | `Module` | `spi/` |

## Requirements

JDK 25.

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

## CommandArguments domain primitive

`CommandArguments` (in `hosh.spi`) is the typed wrapper around the list of arguments passed to `Command#run`. Commands must use this — never raw `List<String>`.

### CommandArguments methods

| Method | Description |
|---|---|
| `CommandArguments.of(String... values)` | Factory for tests and internal use |
| `isEmpty()` | True if no arguments were passed |
| `size()` | Number of arguments |
| `get(int index)` | Returns the argument at `index`; programming error if out of bounds |
| `stream()` | Stream over arguments |
| `iterator()` | Iterable support |

### CommandArgument methods (single argument)

Each element is a `CommandArgument` — a plain string with safe typed accessors. All conversions return `Optional`/`OptionalInt`/`OptionalLong` so callers must handle parse failures explicitly.

| Method | Return type | Description |
|---|---|---|
| `asString()` | `String` | Raw string value |
| `asKey()` | `Key` | Converts to a record key via `Keys.of(value)` |
| `asLong()` | `OptionalLong` | Parses as `long`; empty if not a valid number |
| `asInt()` | `OptionalInt` | Parses as `int`; empty if not a valid number |
| `asDuration()` | `Optional<Duration>` | Parses ISO-8601 duration (with or without `PT` prefix); empty if invalid |
| `asPath(State state)` | `Path` | Resolves relative to `state.getCwd()`; always returns an absolute, normalized path |

### Usage pattern in a command

```java
@Override
public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err, State state) {
    if (args.size() != 1) {
        err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: mycommand <arg>")));
        return ExitStatus.error();
    }
    String value = args.get(0).asString();
    // ...
}
```

## Key architecture notes

- Commands communicate via typed records (not raw strings). Basic types: text, number, path, duration, instant.
- Pipeline stages run concurrently; `Supervisor` manages virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
- Inter-stage data flows through `PipelineChannel` backed by `LinkedTransferQueue`.
- Strict error handling by default — equivalent to `set -euo pipefail` in bash.
- Uses Java Platform Module System (`module-info.java` in every module).
- Each command is a separate class implementing `Command` from `hosh.spi`.
- To add a new command: create the class in the appropriate module, register it in that module's `Module` implementation.

## Code style

- Use tabs, not whitespaces in both Java and XML (checkstyle will fail).
- Java 25, no Kotlin, no Gradle.
- Zero SonarQube bugs/smells policy.
- Checkstyle enforced for any file under src/main and src/test (`checkstyle.xml` at root).
- Prefer explicit over clever. Fail fast on unhandled cases.
- No `sun.misc.Unsafe` or internal JDK APIs.

## Testing

- All features are covered by unit tests.
- Check always the happy path at minimum.
- Tests are written in JUnit 5, Mockito, and AssertJ.
- All tests have `// Given` `// When` `// Then` sections.
- The instance of the class under test is always called `sut` (system under test).
- Acceptance tests use the built jar and run hosh scripts end-to-end.
- Mutation testing via PIT (`pitest-maven`).
- Use @Parametrized tests instead of copy pasting tests
    if @ValueSource is not enough, ask how to prooced
    generally ArgumentSource is fine, but the testcase must have a name
