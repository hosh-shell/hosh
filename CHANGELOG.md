# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
- introducing 'benchmark' command (lots of sharp edges)

## [0.0.21] - 2019-02-04
- generalized pipelines e.g. 'ls | drop 1 | take 1 | count'
- more powerful grammar e.g. nested wrapper, wrapper of pipeline
- Java commands can pipe data into native commands e.g. 'lines pom.xml | /usr/bin/wc -l'
- 'cat' has been renamed to 'lines'

## [0.0.20] - 2019-01-30
- preliminary ctrl-C support
- allowing string in double quotes ""
- new command 'count'
- improved memory usage

## [0.0.19] - 2019-01-27
- introducing pipe operator (i.e. ls | sort name) just for 2 commands
  and some commands like 'enumerate', 'sort', 'filter', 'take', 'drop'
- using alphanum to sort paths
- 'rand', infinite stream of random integers

## [0.0.18] - 2018-10-20
- Hosh now requires Java 11
- lazy resolving variables at runtime
- removing glob pattern from 'find'
  (the idea is to use pipelines to filter paths)
- 'ps' command
- 'kill' command
- 'quit' as alias of 'exit'

## [0.0.17] - 2018-08-28
- 'find' command
- use jline-jansi

## [0.0.16] - 2018-08-28
- improved 'ls' command
- improved humanized size
- improved windows support
- maven wrapper support

## [0.0.15] - 2018-08-03
- fixed 'exit' in REPL

## [0.0.14] - 2018-07-30
- nice exit handling: hosh now behaves like 'set -e' in bash
- enable logging by defining OS env *HOSH_LOG_LEVEL*, OFF by default
- dropped logback, final uberjar is ~600KB smaller
- README.md, LICENSE.md, CHANGELOG.md into uberjar

## [0.0.13] - 2018-07-29
### Added
- ExitStatus API
- 'sleep millis' command

## [0.0.12] - 2018-07-26
- naive filesystem completer

## [0.0.11] - 2018-07-24
- external commands absolute path

## [0.0.10] - 2018-07-24
- external commands
