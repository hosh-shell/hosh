# Hosh

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![CI](https://github.com/hosh-shell/hosh/workflows/CI/badge.svg?branch:master)](https://github.com/hosh-shell/hosh/actions?query=branch:master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hosh%3Ahosh-parent&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=hosh-shell_hosh)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=hosh%3Ahosh-parent&metric=coverage)](https://sonarcloud.io/summary/new_code?id=hosh%3Ahosh-parent)
[![CII Best Practices](https://bestpractices.coreinfrastructure.org/projects/4646/badge)](https://bestpractices.coreinfrastructure.org/projects/4646)

## Features

**H**uman **O**riented **SH**ell, is an experimental shell, featuring:

- **portability**¹
    - written in Java 25, distributed as [Uber-JAR](https://imagej.net/Uber-JAR) and soon Jlink
    - works out-of-the-box in Windows, MacOS, Linux
    - *limited* HTTP 1.1/2.0 client (`http`) and ifconfig clone (`network`) as built-in commands
- **usability as first class citizen**²
    - interactive output displayed as table by default
    - sorting with [natural sort](https://en.wikipedia.org/wiki/Natural_sort_order)
    - ANSI colors by default
    - errors (i.e. stderr) always colored in red
    - file sizes reported by default as KB, MB, GB, ...
    - better history by default
        - record timestamps for each command (see `history` command)
        - ignoring duplicated by default (like `HISTCONTROL=ignoredups` in bash)
        - append to history is incremental and shared between all sessions
        - no limits
- **robust scripts by default**
    - as if running bash scripts with `set -euo pipefail` ([unofficial-strict-mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/))
    - strongly typed (but not statically typed)
        - every value has a type *not everything is a string*
        - avoiding the need to dump/parse fields
        - basic types are: `text`, `number`, `path`, `duration` and `instant`
- **pipelines** built around schema-less records:
    - built-in commands produce *records with well-defined keys*
    - use `| schema` to inspect available keys
    - interoperability with external commands is achieved by using *single-key record* (with key `text`)
- **wrapping commands**, with before/after behavior:
    - `withTime { lines very-big-file.txt | count }` like `time command` in bash
    - `withLock file.lock { command }` run `command` as critical section guarded by `file.lock`
    - `benchmark 10 { command }` run `command` 10 times and then report best/worst/average execution time
- **built with modern tooling and concepts**
    - designed to be compatible with *Java Platform Module System* (i.e. Jigsaw)
    - designed to be compatible with [Project Loom](https://wiki.openjdk.java.net/display/loom/Main)
      as *commands run in isolated thread and communicate only via immutable messages*
    - *Fitness Functions* from *Evolutionary Architecture* ISBN-13: 978-1491986363)

¹ it is not intended to conform to IEEE POSIX P1003.2/ISO 9945.2 Shell and Tools standard

² much more design and work is needed in this area

## Getting started

Requirements: JDK25

```
$ ./mvwn clean verify
$ java -jar main/target/hosh.jar
hosh> echo "hello world!"
hello world!
hosh>
```

## Examples

### Sorting

Sorting is always performed using a well-defined key:
```
hosh> ls
# unsorted, following local file-system order
...
hosh> ls | schema
path size
hosh> ls | sort size
# files sorted by size
...
hosh> ls | sort path
# files sorted by alphanum algorithm
...
```

### Find top-n files by size

Walk is able to recursively walk a directory and its subdirectories, providing
file name and size:
```
hosh> walk . | schema
path size
...
```

By sorting the output of `walk` it is trivial to detect the biggest files:
```
hosh> walk . | sort size desc | take 5
aaa 2,5MB
bbb 1MB
ccc 1MB
ddd 1MB
eee 1MB
```


### HTTP

Stream line by line a TSV file via HTTPS, take first 10 lines, split each line by tab yielding a 1-indexed record and finally show a subset of keys.

Bash + wget + awk:

```
bash$ wget -q -O - -- https://hosh-shell.github.io/sample.tsv | head -n 10 | awk -v OFS='\t' '{print $3, $1, $2}'
```

Hosh (no external commands):

```
hosh> http https://hosh-shell.github.io/sample.tsv | take 10 | split text '\\t' | select 3 1 2
```

### Glob expansion and lambda blocks

To recursively remove all `.class` files in `target`:

`hosh> walk target/ | glob '*.class' | { path -> rm ${path}; echo removed ${path} }`

`{ path -> ... }` is lambda syntax, inside this scope is possible to use `${path}`.

### Parsing

It is possible to create records by using `regex` built-in with capturing groups:

```
hosh> git config -l | schema
text
...
hosh> git config -l | regex text '(?<key>.+)=(?<value>.+)' | take 2
key                value
credential.helper  osxkeychain
user.name          Davide Angelocola
hosh> hosh> git config -l | regex text '(?<key>.+)=(?<value>.+)' | take 2 | schema
key value
key value
```

## Inspired by

- [Collection Pipeline](https://www.martinfowler.com/articles/collection-pipeline/)
- [Bash Pitfalls](https://mywiki.wooledge.org/BashPitfalls)
- [PowerShell](https://docs.microsoft.com/en-us/powershell/)
- [Erlang](https://www.rabbitmq.com/resources/armstrong.pdf)
- [KScript (Kotlin library)](https://github.com/holgerbrandl/kscript)
- [Ammonite (Scala)](https://ammonite.io)
- [Script (Go library)](https://github.com/bitfield/script)

And some nice UI features from:
- [Nushell (Rust)](https://github.com/nushell/nushell)
- [Elvish (Go)](https://elv.sh)
- [Fish (C++)](https://fishshell.com)

## License

[MIT License](LICENSE.md)

## Development

### Requirements

JDK 25.

### Build

Full build with all fitness and acceptance tests:
`$ ./mvnw clean verify`

Quick build (2x faster on my machine):
`$ ./mvnw -Pskip-slow-tests clean verify`

### Run

`$ java -jar main/target/hosh.jar`

### Debug

`$ java -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar main/target/hosh.jar`

### Logging

Hosh uses `java.util.logging` (to not require additional dependencies). `HOSH_LOG_LEVEL` controls
logging behaviour according
to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default,
logging is disabled, to enable it:

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

### Mutation testing

`./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`

## Sponsors

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/java/profiler?from=hosh)
[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=hosh%3Ahosh-parent)
