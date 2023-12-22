Hosh Internals
====

High level architecture
---

Hosh is a modular shell written in Java.

The `main` maven module contains the main method and all the wiring is done there.

The `spi` maven modules contains API (mostly interfaces and annotations), that must be used to write
commands. Most of the functionalities are provided by commands (see `hosh.spi.Command`)
that are injected by instances of `hosh.spi.Module`.

The `runtime` maven module is a huge module that contains most of the internals classes, including:
- the ANTLR4 parser, see `HoshParser.g4` and `HoshLexer.g4`;
- the compiler, see `hosh.runtime.Compiler`;
- the interpreter, see `hosh.runtime.Interpreter`;

Diagram:
```
+------+                  Module 1   
| MAIN |  ---> RUNTIME    Module 2 ---> SPI 
+------+                  .....
```

Modules must be implemented with only the `spi` classes, they are not allowed to see internals.
This rule is enforced by the **module-info.java** classes.
There are some sample modules implemented as part of the main project (see under `modules`): 
those are the extension point of the shell via `ModuleLayer`. 

Decisions
---

Commands follow the UNIX philosophy: they are small, simple and focused in *one* task. 
They are combined using the `|` character (surprise?). Every command can be a source, a sink or both.

Example, consider the following command:

`hosh> ls | sort size desc | take 3`

in this case, `ls` is a source, `sort` is a processor and `take` is the sink (terminal operation).
A command that wants to output records will use the `hosh.spi.OutputChannel` interface, whereas a command
that wants to consume records will use `hosh.spi.InputChannel`.

It is possible to discover the schema of every command with the `schema` command:

`hosh> ls | schema`

One of the most important design choices of Hosh was to rely on threads and message passing 
to implement the pipelines.  
Every command run on a separate thread (later on **virtual threads**).
Messages are implemented by the `hosh.spi.Record` interface. Every instance of this class is 
fully immutable: mutator methods return new instances.

Another important design choice was to give a well-defined keys (i.e. schema) to every command. 
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

Native commands, like `ifconfig` are controlled in a separate thread
that just copy output lines as single key records, with key `text`.

Built-in commands are implemented in "normal" Java and for now they are quite limited. 
They are implemented in the `modules` directory.

Compiler internals
---

The `hosh.runtime.Compiler` is responsible to transform the parse-tree from ANTLR 
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

Compiler is also responsible to resolve commands into either:
- built-in like `schema`;
- `ExternalCommand` for external commands like `ifconfig`, `vim`;

For both cases it uses the `hosh.runtime.CommandResolver`. 

The compiler also generates special instances to use the same `Command` for all constructs 
of the language. 

More precisely:
- `SequenceCommand`, used for `cmd1; cmd2`;
- `PipelineCommand`, used for `cmd1 | cdm2`;
- `DefaultCommandDecorator`, used for wrapper commands `cmd1 { cmd2 }`;
- `LambdaCommand`, used for `cmd1 | { key -> cmd2 ${key} }`.

Interpreter
---

`hosh.runtime.Interpreter` is responsible to run the `Program` produced by the compiler. 

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

As important implementation detail, eval uses a recursive supervision strategy 
to handle resources like process and threads and external signals like CTRL-C. 
It is implemented by the class `hosh.runtime.Supervisor`.

Dependencies
---

- JDK 17+ with modules (but without jlink, for now);
- Apache Maven 3.8+;
- ANTLR4;
- JLine to provide Terminal and History support;
- JUnit5 + Mockito + AssertJ + equalsverifier;
- archunit to enforce some good fitness functions (i.e. all commands must be documented);
- pitest for mutation based testing;
- quicktheories (very limited usage for now); 
- checkstyle. Why? To avoid silly commits + builds just to remove unused imports and to reformat code...
- mockserver to test some HTTP commands;
- SLF4J... just the simple module (for unit testing);
- JOY for JSON (still not wired up).

