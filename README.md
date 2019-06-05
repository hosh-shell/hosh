# Human Oriented SHell

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![Build Status](https://dev.azure.com/davideangelocola/hosh/_apis/build/status/dfa1.hosh?branchName=master)](https://dev.azure.com/davideangelocola/hosh/_build/latest?definitionId=1&branchName=master) [![DepShield Badge](https://depshield.sonatype.org/badges/dfa1/hosh/depshield.svg)](https://depshield.github.io)


## Main features
- written in Java 11
    - distributed as uber-jar or docker image
- portable
    - works out-of-the-box in Windows, MacOS and Linux
    - **it is not intended** to conform to IEEE POSIX P1003.2/ISO 9945.2 Shell and Tools standard
- robust scripts by default
    - like running bash scripts with `set -euo pipefail` (see [unofficial-strict-mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/))
- usability features (although much more work is needed in this area)
    - ANSI colors
    - stderr always colored in red
    - sorting using http://davekoelle.com/alphanum.html
    - file sizes reported by default using KB, MB, etc
    - by default history works like bash with `HISTCONTROL=ignoredups`
- pipelines built around schema-less records
    - built-in commands produce records with well defined keys
    - interoperability is achieved by using single-key records with "line" as value
    - `lines pom.xml | enumerate | take 10`
- wrappers
    - grouping commands, with before/after behavior
    - `withTime { lines pom.xml | sink }`
    - `withLock file.lock { ... }`

## Examples

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

Stream line by line a TSV file via HTTPS, take first 10 lines, split each line by tab yielding a 1-indexed record and finally show a subset of keys:

```
hosh> http https://git.io/v9MjZ | take 10 | split text '\\t' | select 10 1 12
```

that could be translated to the following UNIX pipeline:

```
bash$ wget -q -O - -- https://git.io/v9MjZ | head -n 10 | awk -v OFS='\t' '{print $10, $1, $12}'
```

## Requirements

Java 11

## Build

`$ ./mvnw clean verify`

## Run

`$ java -jar target/dist/hosh.jar`

## Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar target/dist/hosh.jar`

## Logging

`$ HOSH_LOG_LEVEL=debug java -jar target/dist/hosh.jar`

## Docker support

Preliminary docker support using [adoptopenjdk](https://adoptopenjdk.net/) with [alpine-jre](https://hub.docker.com/r/adoptopenjdk/openjdk11):

`$ ./mvnw -Pdocker clean verify`

`$ docker run -it $(docker image ls hosh --quiet  | head -n 1)`

## Eclipse support

Project specific settings can be found under `./eclipse` directory.

## Inspired by
- https://www.martinfowler.com/articles/collection-pipeline/
- https://mywiki.wooledge.org/BashPitfalls
- https://zsh.org
- https://fishshell.com

