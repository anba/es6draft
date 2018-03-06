es6draft
========

ECMAScript compiler and runtime written in Java.

[![Build Status](https://travis-ci.org/anba/es6draft.png?branch=master)](https://travis-ci.org/anba/es6draft)

## Implementation Status ##

Implements [ES2018 Draft 2018-10-25][ecma262], [ES2018 Intl Draft 2018-10-24][ecma402], and some of the current [proposals][proposals].

ECMAScript proposals below stage 3 are not enabled by default. To enable them use the `--stage` command line parameter.

Note: Support of stage 2 (or below) proposals is highly experimental (cf. [TC39 Process][process]), implementation incompatibilities are to be expected.

## Build Instructions and Shell ##

### Prerequisites ###

* Download and install [JDK 8 or later][java]
* Download and install [Apache Maven 3 or later][maven]
* Set-up the `JAVA_HOME` environment variable to point to the JDK installation directory

### Build Instructions ###

* Clone this repository `git clone https://github.com/anba/es6draft.git && cd es6draft`
* Create the executable with `mvn package`

### Shell ###

* Start the shell using either `./bin/es6draft` or `.\bin\es6draft.bat` on Windows&reg;.
* `./bin/es6draft --help` prints the available options.


## Test Suites ##

### Test262 Test Suite ###

To start the [Test262] [test262] test runner select the `test262` Maven profile and set the
`TEST262_PATH` environment variable or use the `-Dtest262.path` parameter.

```
export TEST262_PATH = <test262 main directory>
mvn test -P test262

or:
mvn test -P test262 -Dtest262.path=<test262 main directory>
```

The `-Dtest262.include` parameter can be used to select specific test cases:
```
mvn test -P test262 -Dtest262.include="test/built-ins/Array/prototype/**/*.js"

mvn test -P test262 -Dtest262.include="test/built-ins/{Boolean\,Number}/prototype/**/*.js"
```

The `-Dtest262.exclude` parameter allows to exclude test cases:
```
mvn test -P test262 -Dtest262.exclude="test/built-ins/**/*.js"
```

See [here](src/test/resources/test-configuration.properties) for the test262 default configuration.


### External Test Suites ###

Additional test suites are available to run tests from [ChakraCore][chakra], [Mozilla][mozilla], [V8][v8], and [WebKit][webkit] using the `external` Maven profile.

The following environment variables need to be set to run these tests:
* `CHAKRA_PATH`: ChakraCore main directory
* `MOZILLA_PATH`: Mozilla-Central main directory
* `V8_PATH`: V8 main directory
* `WEBKIT_PATH`: WebKit main directory

Alternatively, the `-Dchakra.path=...`, `-Dmozilla.path=...`, `-Dv8.path=...`, and `-Dwebkit.path=...` parameters can be used.

To skip an external test, use `-D<name>.skip=true`. For example to run only the Mozilla tests, use:
```
mvn test -P external -Dmozilla.path=<...> -Dchakra.skip=true -Dv8.skip=true -Dwebkit.skip=true
```


### Built-in Test Suite ###

The built-in test suite is run when no other Maven profile was selected. In other words, it is run
when the command `mvn test` is used.


[ecma262]: https://tc39.github.io/ecma262/
[ecma402]: https://tc39.github.io/ecma402/
[proposals]: https://github.com/tc39/proposals
[process]: https://tc39.github.io/process-document/
[java]: https://java.sun.com/
[maven]: https://maven.apache.org/download.cgi
[test262]: https://github.com/tc39/test262/
[chakra]: https://github.com/Microsoft/ChakraCore/
[mozilla]: https://github.com/mozilla/gecko-dev/
[v8]: https://github.com/v8/v8/
[webkit]: https://www.webkit.org/building/checkout.html
