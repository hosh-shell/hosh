Hosh Internals
====

High level architecture
---

Hosh is a modular shell written in Java.

The `main` maven module contains the main method and all the wiring is done there.

The `spi` maven module contains the public API (mostly interfaces and annotations) used to write
commands. Most of the functionality is provided by commands (see `hosh.spi.Command`)
that are registered by instances of `hosh.spi.Module`.

The `runtime` maven module contains most of the internals, including:

- the ANTLR4 parser, see `HoshParser.g4` and `HoshLexer.g4`;
- the compiler, see `hosh.runtime.Compiler`;
- the interpreter, see `hosh.runtime.Interpreter`;

Diagram:

```
+------+                  Module 1
| MAIN |  ---> RUNTIME    Module 2 ---> SPI
+------+                  .....
```

Modules must be implemented using only the `spi` classes; they are not allowed to access runtime internals.
This rule is enforced by the **module-info.java** descriptors.
There are sample modules implemented as part of the main project (see under `modules`):
those are the extension points of the shell via `ModuleLayer`.

Decisions
---

Commands follow the UNIX philosophy: they are small, simple and focused on *one* task.
They are combined using the `|` character (surprise?). Every command can be a source, a sink or both.

Example, consider the following command:

`hosh> ls | sort size desc | take 3`

In this case, `ls` is a source, `sort` is a processor and `take` is the sink (terminal operation).
A command that wants to output records will use the `hosh.spi.OutputChannel` interface, whereas a command
that wants to consume records will use `hosh.spi.InputChannel`.

It is possible to discover the schema of every command with the `schema` command:

`hosh> ls | schema`

One of the most important design choices of Hosh was to rely on threads and message passing
to implement the pipelines.
Every command runs on a separate thread (virtual threads via `Executors.newVirtualThreadPerTaskExecutor()`).
Messages are implemented by the `hosh.spi.Record` interface. Every instance of this class is
fully immutable: mutator methods return new instances.

Another important design choice was to give well-defined keys (i.e. a schema) to every command.
For example, the command `ls` produces records with the following code:

```
Record entry = Records.builder()
        .entry(Keys.PATH, Values.ofPath(path.getFileName()))
        .entry(Keys.SIZE, size)
        .entry(Keys.CREATED, Values.ofInstant(attributes.creationTime().toInstant()))
        .entry(Keys.MODIFIED, Values.ofInstant(attributes.lastModifiedTime().toInstant()))
        .entry(Keys.ACCESSED, Values.ofInstant(attributes.lastAccessTime().toInstant()))
        .build();
```

`Keys.SIZE` is a well-known key and global immutable object.

Native commands, like `ifconfig`, are controlled in a separate thread
that just copies output lines as single-key records, with key `text`.

Built-in commands are implemented in "normal" Java and they live under the `modules` directory.

Compiler internals
---

The `hosh.runtime.Compiler` is responsible for transforming the parse-tree from ANTLR
into something runnable. The output is a data class named `Program`:

```
public Program compile(String input) {
    Parser parser = new Parser();
    ProgramContext programContext = parser.parse(input);
    List<Statement> statements = new ArrayList<>();
    for (StmtContext ctx : programContext.stmt()) {
        Statement statement = compileStatement(ctx);
        statements.add(statement);
    }
    return new Program(statements);
}
```

The compiler is also responsible for resolving commands into either:

- built-ins like `schema`;
- `ExternalCommand` for external commands like `ifconfig`, `vim`;

For both cases it uses `hosh.runtime.CommandResolver`.

The compiler also generates special instances to use the same `Command` interface for all language constructs.

More precisely:

- `SequenceCommand`, used for `cmd1; cmd2`;
- `PipelineCommand`, used for `cmd1 | cmd2`;
- `DefaultCommandDecorator`, used for wrapper commands `cmd1 { cmd2 }`;
- `LambdaCommand`, used for `cmd1 | { key -> cmd2 ${key} }`.

Interpreter
---

`hosh.runtime.Interpreter` is responsible for running the `Program` produced by the compiler.

The implementation is something like:

```
public ExitStatus eval(Compiler.Program program, OutputChannel out, OutputChannel err) {
    ExitStatus exitStatus = ExitStatus.success();
    for (Compiler.Statement statement : program.getStatements()) {
        exitStatus = eval(statement, out, err);
        if (userRequestedExit() || lastCommandFailed(exitStatus)) {
            break;
        }
    }
    return exitStatus;
}
```

As an important implementation detail, `eval` uses a recursive supervision strategy
to handle resources like processes and threads and external signals like CTRL-C.
It is implemented by the class `hosh.runtime.Supervisor`.

The `Interpreter` is also responsible for dependency injection: before executing a command it inspects
which SPI awareness interfaces the command implements and injects the relevant dependencies
(e.g. `State`, `LineReader`, `Terminal`, `History`). The supported injection interfaces are in `hosh.spi`:

- `StateAware` — grants read access to shell state
- `StateMutatorAware` — grants write access to shell state
- `LineReaderAware` — grants access to JLine `LineReader` (for interactive input)
- `TerminalAware` — grants access to JLine `Terminal`
- `HistoryAware` — grants access to JLine `History`
- `VersionAware` — grants access to shell version

Note: `hosh.runtime.InterpreterAware` is an **internal** interface (in `hosh.runtime`, not `hosh.spi`) used
only by runtime-internal commands that need access to the `Interpreter` itself (e.g. `LambdaCommand`).
Module authors must not implement this interface.

Channels
---

Channels are the plumbing between pipeline stages. Several implementations exist in `hosh.runtime`:

- `PipelineChannel` — connects two stages in a pipeline; backed by `LinkedTransferQueue` for backpressure;
- `ConsoleChannel` — writes records to the terminal (using `AutoTableChannel` for formatting);
- `AutoTableChannel` — auto-aligns records into a table layout for human-readable output;
- `CancellableChannel` — wraps another channel and stops emission when a cancellation signal is received;
- `NullChannel` — discards all records (used in tests or as `/dev/null` equivalent).

Prompt subsystem
---

The prompt is rendered by composing multiple `PromptProvider` implementations in `hosh.runtime.prompt`:

- `DefaultPromptProvider` — default prompt with path and status indicator;
- `GitCurrentBranchPromptProvider` — appends the current Git branch when inside a repo;
- `HostnamePromptProvider` — appends the machine hostname;
- `UserPromptProvider` — appends the current username;
- `LiteralPromptProvider` — appends a fixed literal string.

`CompositePromptProvider` chains multiple providers together. `StyledPrompt` applies ANSI styling.

Tab completion
---

Three JLine `Completer` implementations cooperate for tab completion in `hosh.runtime`:

- `CommandCompleter` — completes registered command names;
- `FileSystemCompleter` — completes file and directory paths;
- `VariableExpansionCompleter` — completes `${VAR}` variable references from shell state.

Bootstrap
---

`hosh.runtime.BootstrapBuiltins` registers the built-in commands (e.g. `schema`, `help`) into the
command registry before any module is loaded.

`hosh.runtime.BootstrapVariables` populates the initial shell state (e.g. `HOME`, `PATH`) from the
process environment.

`hosh.runtime.PathInitializer` resolves the initial working directory.

Dependencies
---

- JDK 25 with modules;
- Apache Maven 3.9+;
- ANTLR4;
- JLine to provide Terminal and History support;
- JUnit5 + Mockito + AssertJ + equalsverifier (test only);
- archunit to enforce fitness functions, e.g. all commands must be documented (test only);
- pitest for mutation-based testing;
- quicktheories for property-based testing (test only);
- checkstyle — to prevent unused imports and enforce consistent formatting;
- mockserver to test HTTP commands (test only);
- SLF4J — simple binding used (test only);
- JOY for JSON (declared as a dependency, not yet wired up).
