es6draft
========

Experimental ECMAScript 6 compiler and runtime written in Java.

### Project Scope ###

Goals:
* Provide a test environment for the most recent ES6 drafts

Non-Goals:
* Fast, optimized ECMAScript 6 runtime or Java interoperability


### Build Instructions and Shell ###

The following environment variables need to be set to run the test cases:
* `TEST262_PATH`: test262 main directory
* `MOZ_JSTESTS`: js-tests directory (typically `$MOZ/js/src/tests`)
* `MOZ_JITTESTS`: jit-test directory (typically `$MOZ/js/src/jit-test`)
* `V8_INTL`: intl tests directory (typically `$V8/test/intl`)
* `V8_MJSUNIT`: mjs-unit directory (typically `$V8/test/mjsunit`)
* `V8_WEBKIT`: webkit tests directory (typically `$V8/test/webkit`)
* `TRACEUR_TEST`: traceur-test directory (typically `$TRACEUR/test`)
    
Alternatively skip the tests with `mvn -DskipTests=true package`. 

To start the shell, use `./src/main/bin/repl.sh`.


### Implementation Status ###

[ECMAScript 6 draft rev. 19] [es6drafts] and additionally:
* Tail calls (currently limited to user-defined functions)

[ECMAScript Internationalization API 2.0, draft 2013-02-28] [intldrafts]:
* basic support using the [ICU4J] [icu] library
* subclassing intentionally restricted to ES6 classes


[es6drafts]: http://wiki.ecmascript.org/doku.php?id=harmony:specification_drafts "Draft Specification for ES.next"
[intldrafts]: http://wiki.ecmascript.org/doku.php?id=globalization:specification_drafts "Specification Drafts for ECMAScript Internationalization API"
[icu]: http://site.icu-project.org/
