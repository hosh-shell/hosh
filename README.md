# Human Oriented SHell

## Main features
- written in Java 11
- portable, works out-of-the-box in Windows, MacOS and Linux (although much more work is needed here)
- pipelines built around schemaless records
    - interoperability is achieved by using single-key records with "line" as value
    - `lines pom.xml | enumerate | take 10`
- wrappers
    - `withTime { lines pom.xml | sink }`
- usability features (much more work is needed here)
    - ANSI colors
    - stderr always colored in red
    - sorting using http://davekoelle.com/alphanum.html
    - file sizes reported using KB, MB, etc
- distributed as single-jar
- MIT license

## Inspired by
- https://michaelfeathers.silvrback.com/collection-pipelines-the-revenge-of-c
- https://www.martinfowler.com/articles/collection-pipeline/
- https://fishshell.com/
- http://mywiki.wooledge.org/BashPitfalls

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

## Eclipse support

Project specific settings can be found under `./eclipse` directory.
