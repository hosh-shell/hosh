# Human Oriented SHell

## Main features
- written in Java 11
- portable, works out-of-the-box in Windows, MacOS and Linux
- pipelines built around schemaless records
    - built-in commands produce strongly typed records with well defined keys
    - interoperability is achieved by using single-key records with "line" as value
    - `lines pom.xml | enumerate | take 10`
- wrappers
    - `withTime { lines pom.xml | sink }`
- usability features (although much more work is needed in this area)
    - ANSI colors
    - stderr always colored in red
    - sorting using http://davekoelle.com/alphanum.html
    - file sizes reported using KB, MB, etc
    - by default history works like bash with HISTCONTROL=ignoredups
- distributed as single-jar or docker image
- MIT license


## Inspired by
- https://michaelfeathers.silvrback.com/collection-pipelines-the-revenge-of-c
- https://www.martinfowler.com/articles/collection-pipeline/
- https://fishshell.com/
- http://mywiki.wooledge.org/BashPitfalls
- http://minifesto.org/

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

`$ ./mvnw -Pdocker clean package`

`$ docker image ls hosh`

`$ docker run -it  docker run -it $IMAGE`

## Eclipse support

Project specific settings can be found under `./eclipse` directory.
