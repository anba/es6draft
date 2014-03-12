es6draft
========

ECMAScript 6 compiler and runtime written in Java.

### Project Scope ###

Goals:
* Provide a test environment for the most recent ES6 drafts

Non-Goals:
* Fast, optimized ECMAScript 6 runtime or Java interoperability


### Implementation Status ###

[ECMAScript 6 draft rev. 22] [es6drafts].

[ECMAScript Internationalization API 2.0, draft 2013-02-28] [intldrafts]:
* basic support using the [ICU4J] [icu] library
* subclassing intentionally restricted to ES6 classes


### Build Instructions and Shell ###

The following environment variables need to be set to run the test cases:
* `TEST262_PATH`: test262 main directory
* `MOZILLA_PATH`: mozilla-central main directory
* `V8_PATH`: v8 main directory
* `TRACEUR_PATH`: traceur main directory
    
Alternatively skip the tests with `mvn -DskipTests=true package`. 

To start the shell, use `./bin/es6draft` (or `.\bin\es6draft.bat` on Windows&reg;).


[es6drafts]: http://wiki.ecmascript.org/doku.php?id=harmony:specification_drafts "Draft Specification for ES.next"
[intldrafts]: http://wiki.ecmascript.org/doku.php?id=globalization:specification_drafts "Specification Drafts for ECMAScript Internationalization API"
[icu]: http://site.icu-project.org/
