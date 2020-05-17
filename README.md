# HOSH

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT) [![CI](https://github.com/dfa1/hosh/workflows/CI/badge.svg)](https://github.com/dfa1/hosh/actions?query=workflow%3ACI) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=alert_status)](https://sonarcloud.io/dashboard?id=dfa1_hosh)
 [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dfa1_hosh&metric=coverage)](https://sonarcloud.io/dashboard?id=dfa1_hosh)


## Main features

Hosh is an experimental shell written in Java, featuring: 

- **portability**¹
    - works out-of-the-box in Windows, MacOS, Linux 
    - written in Java 11, distributed as [Uber-JAR](https://imagej.net/Uber-JAR)
- **usability as first class citizen** (much more design and work is needed in this area)
    - sorting with [alphanum](http://davekoelle.com/alphanum.html)
    - ANSI colors by default
    - errors always colored in red
    - file sizes reported by default as KB, MB, GB, ...
    - [better history by default](https://sanctum.geek.nz/arabesque/better-bash-history/)
       - record timestamps for each command (see `history` command)
       - `HISTCONTROL=ignoredups`
       - no limits
       - append to history is incremental and shared between all sessions
- **pipelines** built around schema-less records:
    - built-in commands produce records with well defined keys
    - interoperability with external commands is achieved by using single-key records
    - `lines pom.xml | enumerate | take 10`
- **grouping commands**, with before/after behavior 
    - `withTime { lines pom.xml | sink }`
    - `withLock file.lock { ... }`
- **robust scripts by default**
    - as if running bash scripts with `set -euo pipefail` ([unofficial-strict-mode](http://redsymbol.net/articles/unofficial-bash-strict-mode/))

¹ it is not intended to conform to IEEE POSIX P1003.2/ISO 9945.2 Shell and Tools standard

## Examples

### Sorting

Sorting is always performed using a well defined key:
```
hosh> ls
# unsorted, following local filesystem order
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
hosh> walk . | sort desc size | take 5
aaa 2,5MB
bbb 1MB
ccc 1MB
ddd 1MB
eee 1MB
```

Schema is same of `walk`.


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

### Glob expansion and lambda blocks

To recursively remove all `.class` files in `target`:

`hosh> walk target/ | glob '*.class' | { path -> rm ${path}; echo removed ${path} }`

`{ path -> ... }` is lambda syntax, inside this scope is possible to use `${path}`.


## Inspired by

- https://www.martinfowler.com/articles/collection-pipeline/
- https://mywiki.wooledge.org/BashPitfalls
- PowerShell https://docs.microsoft.com/en-us/powershell/
- Zsh https://zsh.org
- Elvish https://elv.sh
- Fish https://fishshell.com

## Similar projects

- rust https://github.com/nushell/nushell
- scala https://ammonite.io
- kotlin https://github.com/holgerbrandl/kscript
- go https://github.com/bitfield/script

## Development

### Requirements

JDK 11

### Build

`$ ./mvnw clean verify`

### Run

`$ java -jar target/dist/hosh.jar`

### Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar target/dist/hosh.jar`

### Logging

Hosh uses `java.util.logging` (to not require additional dependencies). `HOSH_LOG_LEVEL` controls
logging behaviour according to [Level](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Level.html). By default logging is disabled, to enable it:

`$ HOSH_LOG_LEVEL=FINE java -jar target/dist/hosh.jar`

Logging events will be persisted in `$HOME/.hosh.log`.

### Docker support

Preliminary docker support using [adoptopenjdk](https://adoptopenjdk.net/):

`$ ./mvnw -Pdocker clean verify`

`$ docker run -it $(docker image ls hosh --quiet  | head -n 1)`

### Windows 7 UAC

If any test fails with "java.nio.file.FileSystemException: A required privilege is not held by the client."
then:

- run **secpol.msc**
- go to Security Settings|Local Policies|User Rights Assignment|Create symbolic links
- add your user name.
- restart your session.

(see https://stackoverflow.com/a/24353758)

