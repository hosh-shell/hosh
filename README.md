# Human Oriented SHell

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![Build Status](https://dev.azure.com/davideangelocola/hosh/_apis/build/status/dfa1.hosh?branchName=master)](https://dev.azure.com/davideangelocola/hosh/_build/latest?definitionId=1&branchName=master) [![DepShield Badge](https://depshield.sonatype.org/badges/dfa1/hosh/depshield.svg)](https://depshield.github.io)


## Main features
- portable
    - written in Java 11, distributed as [Uber-JAR](https://imagej.net/Uber-JAR)
    - works out-of-the-box in Windows, MacOS and Linux¹
- usability features (although much more work is needed in this area)
    - sorting with [alphanum](http://davekoelle.com/alphanum.html)
    - ANSI colors by default
    - stderr always colored in red
    - file sizes reported by default as KB, MB, GB, ...
    - [better history by default](https://sanctum.geek.nz/arabesque/better-bash-history/)
       - record timestamps for each command (see `history` command)
       - `HISTCONTROL=ignoredups`
       - no limits
       - append to history is incremental and shared between all sessions
       - ctrl-R works as expected
- pipelines built around schema-less records
    - built-in commands produce records with well defined keys
    - interoperability is achieved by using single-key records
    - `lines pom.xml | enumerate | take 10`
- wrappers
    - grouping commands, with before/after behavior
    - `withTime { lines pom.xml | sink }`
    - `withLock file.lock { ... }`
- robust scripts by default
    - like running bash scripts with `set -euo pipefail` (see [unofficial-strict-mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/))

¹ it is not intended to conform to IEEE POSIX P1003.2/ISO 9945.2 Shell and Tools standard

## Examples

### Sorting

Sorting is always performed against a well defined key:
```
hosh> ls
# unsorted, following local filesystem
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

### Parsing

It is possible to create records from text by using `regex` built-in:

```
hosh> git config -l | regex text '(?<name>.+)=(?<value>.+)' | take 3 | table
name                          value
credential.helper             osxkeychain
user.name                     Davide Angelocola
user.email                    davide.angelocola@gmail.com
```

### HTTP

Stream line by line a TSV file via HTTPS, take first 10 lines, split each line by tab yielding a 1-indexed record and finally show a subset of keys.

Bash + wget + awk:

```
bash$ wget -q -O - -- https://git.io/v9MjZ | head -n 10 | awk -v OFS='\t' '{print $10, $1, $12}'
```

Hosh (no external commands):

```
hosh> http https://git.io/v9MjZ | take 10 | split text '\\t' | select 10 1 12
```


## Inspired by

- PowerShell https://docs.microsoft.com/en-us/powershell/
- Zsh https://zsh.org
- https://www.martinfowler.com/articles/collection-pipeline/
- https://mywiki.wooledge.org/BashPitfalls

## Similar projects

- rust https://github.com/nushell/nushell
- scala https://ammonite.io
- kotlin https://github.com/holgerbrandl/kscript
- go https://github.com/bitfield/script

# Development

## Requirements

Java 11

## Build

`$ ./mvnw clean verify`

## Run

`$ java -jar target/dist/hosh.jar`

## Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar target/dist/hosh.jar`

## Logging

Hosh uses `java.util.logging` (to not require additional dependencies). `HOSH_LOG_LEVEL` controls
logging behaviour according to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default logging is disabled, to enable it:

`$ HOSH_LOG_LEVEL=FINE java -jar target/dist/hosh.jar`

Logging events will be persisted in `$HOME/.hosh.log`.

## Eclipse support

Project specific settings can be found under `./eclipse` directory.

## Docker support

Preliminary docker support using [adoptopenjdk](https://adoptopenjdk.net/) with [alpine-jre](https://hub.docker.com/r/adoptopenjdk/openjdk11):

`$ ./mvnw -Pdocker clean verify`

`$ docker run -it $(docker image ls hosh --quiet  | head -n 1)`


