# hosh — Claude guidance

Human Oriented SHell: experimental Java shell, typed records, virtual-thread pipelines. Multi-module Maven project (JDK 25).

Website: <https://hosh-shell.github.io>

## Build

```bash
./mvnw clean verify                          # full build (unit + integration + fitness + acceptance)
./mvnw -Pskip-slow-tests clean verify        # ~2× faster, skip slow tests
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage  # mutation testing
./mvnw clean verify sonar:sonar -Psonar -Dsonar.token=MYTOKEN  # sonar analysis
```

Run: `java -jar main/target/hosh.jar`

Debug:

```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar main/target/hosh.jar
```

## Logging

Hosh uses `java.util.logging`, disabled by default.

```bash
HOSH_LOG_LEVEL=FINE java -jar main/target/hosh.jar
```

Log events written to `$HOME/.hosh.log`.

## Module map

| Module | Purpose |
|---|---|
| `spi/` | Core abstractions: `Command`, `Channel`, `Record`, `ExitStatus`, `Value` subtypes |
| `spi-test-support/` | Test helpers for SPI |
| `test-support/` | JUnit 5 utilities |
| `runtime/` | Shell engine: `Interpreter`, `Supervisor`, ANTLR4 `Compiler`/`Parser` |
| `modules/system/` | echo, sleep, withTimeout, benchmark, … |
| `modules/filesystem/` | ls, cd, walk, cp, mv, rm, find, … |
| `modules/text/` | grep, sort, count, split, join, trim, regex, … |
| `modules/network/` | http, resolve, … |
| `modules/terminal/` | clear, … |
| `modules/history/` | history |
| `main/` | Entry point `Hosh.java`, produces uberjar |

Root package: `hosh`. Key sub-packages: `hosh.spi`, `hosh.runtime`, `hosh.runtime.prompt`, `hosh.runtime.completion`, `hosh.modules.*`.

## Key classes

| Concern | Class | Location |
|---|---|---|
| Entry point | `Hosh` | `main/` |
| Parsing | `Compiler`, `Parser` | `runtime/` |
| ANTLR4 grammar | `Hosh.g4` | `runtime/src/main/antlr4/` |
| Pipeline execution | `Supervisor` | `runtime/` |
| Inter-stage data | `PipelineChannel` | `runtime/` |
| Core record type | `Record`, `Records` | `spi/` |
| Typed values | `Value`, `Keys` | `spi/` |
| Command interface | `Command` | `spi/` |
| Command arguments | `CommandArguments`, `CommandArgument` | `spi/` |
| Channels | `InputChannel`, `OutputChannel` | `spi/` |
| Module registration | `Module` | `spi/` |

## Architecture constraints

- Commands communicate via typed `Record`s — never raw strings across subsystem boundaries.
- Pipeline stages run concurrently on virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`).
- Inter-stage data flows through `PipelineChannel` backed by `LinkedTransferQueue`.
- Strict error handling by default — equivalent to `set -euo pipefail` in bash.
- Modules depend only on `spi/` — never on `runtime/` internals (enforced by `module-info.java`).
- Uses Java Platform Module System (`module-info.java` in every module).
- To add a command: create class implementing `Command`, register in the module's `Module` impl.
- Dependency injection happens in `Interpreter` via awareness interfaces (`StateAware`, `StateMutatorAware`, `LineReaderAware`, `TerminalAware`, `HistoryAware`, `VersionAware`).

## CommandArguments

Always use `CommandArguments` — never raw `List<String>`.

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

### CommandArguments methods

| Method | Description |
|---|---|
| `CommandArguments.of(String... values)` | Factory for tests and internal use |
| `isEmpty()` | True if no arguments |
| `size()` | Number of arguments |
| `get(int index)` | Argument at index; programming error if out of bounds |
| `stream()` | Stream over arguments |

### CommandArgument accessors

| Method | Return type | Description |
|---|---|---|
| `asString()` | `String` | Raw string value |
| `asKey()` | `Key` | Converts via `Keys.of(value)` |
| `asLong()` | `OptionalLong` | Parses as `long`; empty if invalid |
| `asInt()` | `OptionalInt` | Parses as `int`; empty if invalid |
| `asDuration()` | `Optional<Duration>` | Parses ISO-8601 duration; empty if invalid |
| `asPath(State state)` | `Path` | Resolves relative to `state.getCwd()`; absolute, normalized |

## Code style

- Tabs, not spaces (Java and XML). Checkstyle will fail otherwise.
- Checkstyle enforced for all files under `src/main` and `src/test` (`checkstyle.xml` at root).
- Java 25. No Kotlin, no Gradle.
- Zero SonarQube bugs/smells policy.
- No `sun.misc.Unsafe` or internal JDK APIs.
- Prefer explicit over clever. Fail fast on unhandled cases.
- Domain primitives: use `Key`, `Value`, `ExitStatus`, `VariableName`, `CommandName` — never pass raw `String`/primitives across subsystem boundaries.

## Testing

- All features covered by unit tests. Always check the happy path at minimum.
- JUnit 5 + Mockito (BDDMockito) + AssertJ.
- Every test has `// Given` / `// When` / `// Then` sections.
- Class under test is always named `sut`.
- Use `BDDMockito` exclusively: `given(mock.method()).willReturn(value)`. Never the reverse form. Only static-import `given` and `then` — not `willReturn`/`willThrow`.
- Prefer `@ParameterizedTest` over copy-pasting tests. Use `@ValueSource` when possible; `@ArgumentsSource` when more structure needed (test case must have a name).
- Acceptance tests run the built jar end-to-end with hosh scripts.
- Mutation testing via PIT (`pitest-maven`).

## Property-Based Testing (jqwik)

**Known issue:** jqwik 1.9.3 targets JUnit Platform 1.x; project uses JUnit 6 (Platform 6.x). `@Property` tests compile and are structurally correct but the jqwik engine does not execute them at runtime. Track https://github.com/jqwik-team/jqwik/issues for jqwik 2.x.

Write property tests: `@Property` + `@ForAll` for parameters, `@Provide` for custom arbitraries, `Assume.that(...)` for preconditions. See `ValuesTest.java` (spi module) for patterns.

Key properties:
- Comparator contract: reflexivity, antisymmetry, transitivity
- Merge commutativity: `a.merge(b) == b.merge(a)`
- Sort idempotence: `sort(sort(xs)) == sort(xs)`
- Partition: `take(n, xs) + drop(n, xs) == xs`

`UnitTestsFitnessTest` allows `@Property` and `@Provide` alongside standard JUnit annotations.
