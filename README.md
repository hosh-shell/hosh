# Human Oriented SHell

 [![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![Build Status](https://dev.azure.com/davideangelocola/hosh/_apis/build/status/dfa1.hosh?branchName=master)](https://dev.azure.com/davideangelocola/hosh/_build/latest?definitionId=1&branchName=master)

## Main features
- written in Java 11
- portable, works out-of-the-box in Windows, MacOS and Linux
    - it is not intended to conform to IEEE POSIX P1003.2/ISO 9945.2 Shell and Tools standard
- pipelines built around schemaless records
    - built-in commands produce records with well defined keys
    - interoperability is achieved by using single-key records with "line" as value
    - `lines pom.xml | enumerate | take 10`
- wrappers
    - grouping commands, with before/after behaviour
    - `withTime { lines pom.xml | sink }`
- usability features (although much more work is needed in this area)
    - ANSI colors
    - stderr always colored in red
    - sorting using http://davekoelle.com/alphanum.html
    - file sizes reported by default using KB, MB, etc
    - by default history works like bash with HISTCONTROL=ignoredups
- distributed as uber-jar or docker image
- MIT license


## Inspired by
- https://zsh.org
- https://fishshell.com
- https://mywiki.wooledge.org/BashPitfalls
- https://michaelfeathers.silvrback.com/collection-pipelines-the-revenge-of-c
- https://www.martinfowler.com/articles/collection-pipeline/

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

Preliminary docker support (openjdk:11-jre-slim-stretch):

`$ ./mvnw -Pdocker clean verify`

`$ docker run -it $(docker image ls hosh --quiet  | head -n 1)`

## Eclipse support

Project specific settings can be found under `./eclipse` directory.
