es6draft
========

ECMAScript 2015/2016 compiler and runtime written in Java.

[![Build Status](https://travis-ci.org/anba/es6draft.png?branch=master)](https://travis-ci.org/anba/es6draft)

## Implementation Status ##

Complete implementation of [ECMAScript® 2015] [es2015].

Complete implementation of [ECMAScript® 2015 Internationalization API, ECMA-402 2nd edition] [intl].

Implements [ES2016 Draft 2015-12-01] [es2016].

Supports the following [ECMAScript proposals] [proposals]:
* Stage 4:
  * Array.prototype.includes
* Stage 3:
  * Async Functions
  * Exponentiation Operator
  * Object.values/Object.entries
  * SIMD.JS
  * String padding
  * Trailing commas in function parameter lists and calls
* Stage 2:
  * function.sent metaproperty
  * Rest/Spread Properties
* Stage 1:
  * Additional export-from Statements
  * ArrayBuffer.transfer
  * Callable class constructors
  * Class and Property Decorators
  * Class Property Declarations (Only class static properties are currently supported)
  * String.prototype.{trimLeft,trimRight}
  * String#matchAll

ECMAScript proposals are not enabled by default. To enable them use the `--stage` command line parameter.

Note: Support of stage 2 (or below) proposals is highly experimental (cf. [TC39 Process] [process]), implementation incompatibilities are to be expected.

## Build Instructions and Shell ##

### Prerequisites ###

* Download and install [JDK 8 or later] [java]
* Download and install [Apache Maven 3 or later] [maven]
* Set-up the `JAVA_HOME` environment variable to point to the JDK installation directory

### Build Instructions ###

* Clone this repository `git clone https://github.com/anba/es6draft.git && cd es6draft`
* Create the executable with `mvn package`

### Shell ###

* Start the shell using either `./bin/es6draft` or `.\bin\es6draft.bat` on Windows&reg;.
* `./bin/es6draft --help` prints the available options.


## Test Suites ##

### Built-in Test Suite ###

The built-in test suite is run when no other Maven profile was selected. In other words, it is run
when the command `mvn test` is used.


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


### External Tests ###

Additional test suites are available to run tests from [ChakraCore] [chakra], [Mozilla] [mozilla], [Traceur] [traceur], [V8] [v8] and [WebKit] [webkit] using the `external` Maven profile.

The following environment variables need to be set to run these tests:
* `CHAKRA_PATH`: ChakraCore main directory
* `MOZILLA_PATH`: Mozilla-Central main directory
* `TRACEUR_PATH`: Traceur main directory
* `V8_PATH`: V8 main directory
* `WEBKIT_PATH`: WebKit main directory

Alternatively, the `-Dchakra.path=...`, `-Dmozilla.path=...`, `-Dtraceur.path=...`, `-Dv8.path=...` and `-Dwebkit.path=...` parameters can be used.

To skip an external test, use `-D<name>.skip=true`. For example to run only the Traceur feature tests, use:
```
mvn test -P external -Dtraceur.path=<...> -Dchakra.skip=true -Dmozilla.skip=true -Dv8.skip=true -Dwebkit.skip=true
```

[es2015]: http://ecma-international.org/publications/standards/Ecma-262.htm "ECMAScript® 2015 Language Specification"
[es2016]: https://github.com/tc39/ecma262/releases
[intl]: http://ecma-international.org/publications/standards/Ecma-402.htm "ECMAScript® 2015 Internationalization API Specification"
[proposals]: https://github.com/tc39/ecma262#current-proposals
[process]: https://tc39.github.io/process-document/
[icu]: http://site.icu-project.org/
[java]: http://java.sun.com/
[maven]: https://maven.apache.org/download.cgi
[test262]: https://github.com/tc39/test262/
[chakra]: https://github.com/Microsoft/ChakraCore/
[mozilla]: https://github.com/mozilla/gecko-dev/
[traceur]: https://github.com/google/traceur-compiler/
[v8]: https://github.com/v8/v8/
[webkit]: https://www.webkit.org/building/checkout.html
