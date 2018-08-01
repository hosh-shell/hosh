# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]
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
