# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v0.1.4] - 2021-8-07
- bumped jline to 3.20
- skipping autocomplete of commands when current line is not empty
- improving Dockerfile
   + build from source in a consistent environment
   + fetch dependencies in a separate step (for caching!)
   + use multi-stage builds to remove build dependencies
- JDK 16 compatibility

## [v0.1.3] - 2020-11-03

### Added
- `waitSuccess`: runs nested command until first success
    inspired by https://medium.com/@marko.luksa/bash-trick-repeat-last-command-until-success-750a61c43c8a
- `withTimeout`: runs nested command with a timeout
    inspired by https://www.cyberciti.biz/faq/linux-run-a-command-with-a-time-limit/
- new infrastructure for `cmd { ... }`
- JDK 15 compatibility

### Changed
- bumped jline to 3.17.1
- `sleep`: removed 2 args overload
  now it is possible to specify duration with both ISO8601 format and with our custom format (just dropping PT prefix)
  `sleep PT1s`, `sleep 1s` and `sleep 1S` are equivalent.

### Fixed
- `http`: improving error handling
- always destroy underlying native process on InterruptedException

## [v0.1.2] - 2020-07-28

### Added

- (preview feature) hosh interprets the command line as it is typed and uses syntax highlighting to provide feedback to the user.
  Potential errors, that are marked in bold red, include:
    + any syntax error
    + invalid commands (both built-in and external)
- `path`: new command to avoid text-based manipulation of PATH variable
    + `path show` to show all elements of current PATH
    + `path clear` to remove all elements of current PATH
    + `path append /usr/local/bin` to add `/usr/local/bin` as last element of current PATH
    + `path prepend /usr/local/bin` to add `/usr/local/bin` as first element of current PATH

### Changed
- bumped jline to 3.16.0

### Fixed
- fixed regression in `ls | sum size`

## [v0.1.1] - 2020-06-27

### Added

- #265: raspberry pi 4 support
- `freq`: new command to replace pattern 'sort | uniq -c | sort -rn'
  (see example examples/visitors.hosh)
- `last`: new command similar to 'tail -n'
- `max`, `min`: calculate max and min value, works for any value (e.g. timestamp, numeric, strings)
- `sum`: supports size and numeric values
- `confirm`: ask a question and wait for user confirmation
- `ls`: adding creation, modification and last access time
- `ls`: removing ANSI coloring (to be reimplemented later)
- `walk`:
    - follow symlinks by default
    - revert 'fail fast' error handling

### Changed
- changed `groupId` in maven
   dfa1 -> `hosh`
- `http`: integration tests (https://postman-echo.com)
- `sort`: consistent order of arguments
    - `key` is mandatory and always in first position
    - `asc`, `desc` are optional and always in second position
- `table`: merged as "autotable" in interactive mode

### Fixed
- `probe`: fix exit status when content type cannot be determined
- minor fixes
   - ignoring UAC errors in test on Windows
   - stability of integration tests

## [v0.1.0] - 2020-05-31

### Added
- sign jar file with GPG when deploy on GitHub
- `ls`: adding mtime, atime and ctime

### Changed
- introducing Java modules
- `walk`: 'fail fast' error handling
- removing automatic semicolon insertion

### Fixed
- improved error handling of lambda
- fix race condition in `PipelineChannel`

## [v0.0.38] - 2020-02-29

### Added
- preliminary support for Flight Recorder (JEP 328)
  - custom events for command executions
  - improved `location` while using `HOSH_LOG` as well

### Changed
- improved handling of strings

### Fixed
- #205: ability to use '{' (any other character in strings)
   - now it is possible to use `glob '*.{java,jar}'` and much more

## [v0.0.37] - 2020-02-22

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

## [v0.0.36] - 2020-01-30

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
- storing history in `$HOME/.hosh_history` instead of `$HOME/.hosh.history
- dropping trailing spaces during PATH init to avoid `java.nio.file.InvalidPathException`
- improving path normalization (e.g. `c:\\vagrant` into `\\vboxsrv`)

## [v0.0.35] - 2019-11-29

### Added
- improved `sleep` with additional "unit" e.g.:
  - `sleep 15 minutes` now it is valid
  - `sleep PT15M` now it is valid too (https://en.wikipedia.org/wiki/ISO_8601#Durations)
- updated dependencies by enabling https://dependabot.com/

### Fixed
- ensuring that `withLock` removes lock file

### Removed
- ANSI colors in log file

## [v0.0.34] - 2019-11-23

### Added
- command line options --help and --version
- improved history support (fix #147)
- split `Channel` interface (fix #152)
- `InputChannel.iterate()` API (fix #116)

### Fixed
- NPE when starting without PATH (fix #147)

## [v0.0.33] - 2019-06-18

### Added
- introducing `withLock` wrapper
- introducing `join` command
- introducing `sum` command
- improved error messages by using command name prefix (fix #88)
- multiline pipelines (fix #83)

### Fixed
- allowing sequence commands everywhere (fix #130)
- fix symlink support in `ls`

## [v0.0.32] - 2019-05-22

### Added
- variable expansion with fallback
   - `echo ${VAR!hello}` expands to `'hello'` when VAR is not defined
- new command `resolve`
   - similar to `readlink -f` or `realpath`
- new command `input` and `secret`
   - `input FOO` for saving non-secure user input into a variable
   - `secure FOO' like `input` but masking input

## [v0.0.31] - 2019-05-08
- commands must be terminated with ';'
    - script are backward-compatible because Hosh is automatically ending every line with ';'
    - this allows sequence of commands like `ls ; ls`
- new command 'http':
    - GET only
    - HTTP 1.1/2.0
    - detect system proxy
- new command 'trim'

## [v0.0.30] - 2019-05-01
- new commands: 'cp', 'mv', 'rm'
   - no recursion yet
   - no directory operation yet, only files
   - no overwrites
- new command: 'partitions'
- new command: 'probe'
- new command: 'symlink', 'hardlink'
- auto-complete of any executable in PATH
- JUnit 4.x -> 5.x

## [v0.0.29] - 2019-04-19
- removed command 'source'
- 100% built-in commands documented with help and examples
- improved test coverage (thanks @pitest)

## [v0.0.28] - 2019-03-29
- new command 'regex', basically named groups regular expressions to record
- new command 'network', to display all network interfaces in this system
- introducing annotation-based help system
- improving auto-complete of partially specified directories

## [v0.0.27] - 2019-03-17
- new system commands:
   - 'capture' low-level command for implementing later $()
   - 'open' low-level command for implementing later 'cmd > file' and 'cmd >> file'
   - 'watch' preliminary version for watching file-system changes
   - 'timestamp' to insert a timestamp into each incoming record
- 'table' has been colorized

## [v0.0.26] - 2019-03-10
- new system commands: 'set' and 'unset'
- new text commands: 'distinct' and 'duplicated'
- auto-completion of variable expansion, triggered by '${'
- improved auto-completion of directories
- introducing docker support

## [v0.0.25] - 2019-03-01
- support for 'PATHEXT' in Windows
- 'PipelineCommand' has been polished

## [v0.0.24] - 2019-02-18
- improved Windows support
    + resolution of external commands such as "java" will be attempted as "java" then "java.exe"
    + test suite now is compatible with windows
    + using jline-jna instead of jline-jansi
- 'source' and 'sink' commands

## [v0.0.23] - 2019-02-11
- improved ctrl-C handling (#44)
- migrated from SLF4J to JUL

## [v0.0.22] - 2019-02-09
- introduced 'table' command: 'ls | table' for a bit nicer output
- introduced 'benchmark' command (lots of sharp edges)
- detecting and reporting extraneous '}' at the end of statement
- follow symlinks when target of 'find' and 'ls' are symlinks

## [v0.0.21] - 2019-02-04
- generalized pipelines e.g. 'ls | drop 1 | take 1 | count'
- more powerful grammar e.g. nested wrapper, wrapper of pipeline
- Java commands can pipe data into native commands e.g. 'lines pom.xml | /usr/bin/wc -l'
- 'cat' has been renamed to 'lines'

## [v0.0.20] - 2019-01-30
- preliminary ctrl-C support
- allowing string in double quotes ""
- new command 'count'
- improved memory usage

## [v0.0.19] - 2019-01-27
- introducing pipe operator (i.e. ls | sort name) just for 2 commands
  and some commands like 'enumerate', 'sort', 'filter', 'take', 'drop'
- using alphanum to sort paths
- `rand`: infinite stream of random integers

## [v0.0.18] - 2018-10-20
- Hosh now requires Java 11
- lazy resolving variables at runtime
- removing glob pattern from 'find'
  (the idea is to use pipelines to filter paths)
- 'ps' command
- 'kill' command
- 'quit' as alias of 'exit'

## [v0.0.17] - 2018-08-28
- 'find' command
- use jline-jansi

## [v0.0.16] - 2018-08-28
- improved 'ls' command
- improved humanized size
- improved windows support
- maven wrapper support

## [v0.0.15] - 2018-08-03
- fixed 'exit' in REPL

## [v0.0.14] - 2018-07-30
- nice exit handling: hosh now behaves like 'set -e' in bash
- enable logging by defining OS env *HOSH_LOG_LEVEL*, OFF by default
- dropped logback, final uberjar is ~600KB smaller
- README.md, LICENSE.md, CHANGELOG.md into uberjar

## [v0.0.13] - 2018-07-29

- ExitStatus API
- 'sleep millis' command

## [v0.0.12] - 2018-07-26
- naive filesystem completer

## [v0.0.11] - 2018-07-24
- external commands absolute path

## [v0.0.10] - 2018-07-24
- external commands
