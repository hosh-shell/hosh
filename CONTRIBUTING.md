Thanks for contributing to Hosh!

Please be sure that you read the Code of Conduct before contributing to this project and please create a new Issue and
discuss first what your are planning to do for bigger changes.

The overall goal of Hosh is to have a portable, text-based shell for Linux/MacOS/Windows, while showing and testing
features of standard JDK like FileSystem, WatchService, NetworkInterface, etc.

In order to achieve that we have a strong focus on maintainability and high test coverage:

- formatting is enforced automatically by checkstyle: to keep at minimum discussions about it;

- we expect new or modified unit test for every change (written in JUnit5 + Mockito + AssertJ);

- we don't expect usage of specific system commands (e.g. /bin/sh, ifconfig, curl);

- we like idiomatic Java code without too much magic (i.e. explicit "boring" code).

If you have any question please consider asking in https://github.com/hosh-shell/hosh/discussions
For bug reports or specific code related topics create a new issue in https://github.com/hosh-shell/hosh/issues

Thanks!
