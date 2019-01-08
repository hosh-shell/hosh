# Human Oriented SHell

## Requirements

JDK8 for build. JRE8 to run.

## Build

`$ ./mvnw clean package`

## Run

`$ java -jar target/dist/hosh.jar`


## Debug

`$ java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 -jar target/dist/hosh.jar`

## Eclipse support

Project specific settings can be found under './eclipse' directory.
