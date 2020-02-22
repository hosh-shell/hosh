# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.37] - 2020-02-22

### Added
- lambda command
  - alternative to glob expansion
  - `command | { size -> ... ` binding of `size` variable for each incoming record
- using alphanum for generic text too
- new `sort` features:
   - `sort asc key`
   - `sort desc key`
   - improved design of `Values.none()`

### Changed
- switched back to jansi: final jar now is 1.3MB instead of 2.5MB
- azure-pipelines -> github-actions
- enabling parallel test execution
- preliminary introduction of java-module.info

### Fixed
- #212: now it is possible to use several external commands at once in a pipeline

## [0.0.36] - 2020-01-30

### Added
- `glob` for glob pattern matching
  - works as generic filter for any record with `path` key
  - e.g. `walk . | glob '*.java`

### Changed
- "on demand" record creation
   - e.g. `ls | take 1` does only 1 read access to the file system
   - noticeable performance penalty
- `find` has been renamed to `walk`
  - added `size` key, enabling to quickly find the largest file by doing `walk . | sort size`

### Fixed
- storing history in `$HOME/.hosh_history` instead of `$HOME/.hosh.history)
- dropping trailing spaces during PATH init to avoid `java.nio.file.InvalidPathException`
- improving path normalization (e.g. `c:\\vagrant` into `\\vboxsrv`)

## [0.0.35] - 2019-11-29

### Added
- improved `sleep` with additional "unit" e.g.:
  - `sleep 15 minutes` now it is valid
  - `sleep PT15M` now it is valid too (https://en.wikipedia.org/wiki/ISO_8601#Durations)
- updated dependencies by enabling https://dependabot.com/

### Fixed
- ensuring that `withLock` removes lock file

### Removed
- ANSI colors in log file

## [0.0.34] - 2019-11-23

### Added
- command line options --help and --version
- improved history support (fix #147)
- split `Channel` interface (fix #152)
- `InputChannel.iterate()` API (fix #116)

### Fixed
- NPE when starting without PATH (fix #147)

## [0.0.33] - 2019-06-18

### Added
- introducing `withLock` wrapper
- introducing `join` command
- introducing `sum` command
- improved error messages by using command name prefix (fix #88)
- multiline pipelines (fix #83)

### Fixed
- allowing sequence commands everywhere (fix #130)
- fix symlink support in `ls`

## [0.0.32] - 2019-05-22

### Added
- variable expansion with fallback
   - `echo ${VAR!hello}` expands to `'hello'` when VAR is not defined
- new command `resolve`
   - similar to `readlink -f` or `realpath`
- new command `input` and `secret`
   - `input FOO` for saving non-secure user input into a variable
   - `secure FOO' like `input` but masking input

## [0.0.31] - 2019-05-08
- commands must be terminated with ';'
    - script are backward-compatible because Hosh is automatically ending every line with ';'
    - this allows sequence of commands like `ls ; ls`
- new command 'http':
    - GET only
    - HTTP 1.1/2.0
    - detect system proxy
- new command 'trim'

## [0.0.30] - 2019-05-01
- new commands: 'cp', 'mv', 'rm'
   - no recursion yet
   - no directory operation yet, only files
   - no overwrites
- new command: 'partitions'
- new command: 'probe'
- new command: 'symlink', 'hardlink'
- auto-complete of any executable in PATH
- JUnit 4.x -> 5.x

## [0.0.29] - 2019-04-19
- removed command 'source'
- 100% built-in commands documented with help and examples
- improved test coverage (thanks @pitest)

## [0.0.28] - 2019-03-29
- new command 'regex', basically named groups regexp to record
- new command 'network', to display all network interfaces in this system
- introducing annotation-based help system
- improving auto-complete of partially specified directories

## [0.0.27] - 2019-03-17
- new system commands:
   - 'capture' low-level command for implementing later $()
   - 'open' low-level command for implementing later 'cmd > file' and 'cmd >> file'
   - 'watch' preliminary version for watching file-system changes
   - 'timestamp' to insert a timestamp into each incoming record
- 'table' has been colorized

## [0.0.26] - 2019-03-10
- new system commands: 'set' and 'unset'
- new text commands: 'distinct' and 'duplicated'
- auto-completion of variable expansion, triggered by '${'
- improved auto-completion of directories
- introducing docker support

## [0.0.25] - 2019-03-01
- support for 'PATHEXT' in Windows
- 'PipelineCommand' has been polished

## [0.0.24] - 2019-02-18
- improved Windows support
    + resolution of external commands such as "java" will be attempted as "java" then "java.exe"
    + test suite now is compatible with windows
    + using jline-jna instead of jline-jansi
- 'source' and 'sink' commands

## [0.0.23] - 2019-02-11
- improved ctrl-C handling (#44)
- migrated from SLF4J to JUL

## [0.0.22] - 2019-02-09
- introduced 'table' command: 'ls | table' for a bit nicer output
- introduced 'benchmark' command (lots of sharp edges)
- detecting and reporting extraneous '}' at the end of statement
- follow symlinks when target of 'find' and 'ls' are symlinks

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
