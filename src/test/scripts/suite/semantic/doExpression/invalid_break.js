/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, fail
} = Assert;

// Note: break/continue are currently disallowed in loop-headers.

assertSyntaxError(`for (var k in do { break; }) ;`);
assertSyntaxError(`for (var k in do { continue; }) ;`);
assertSyntaxError(`L: for (var k in do { break L; }) ;`);

assertSyntaxError(`for (var {k = do { break; }} in []) ;`);
assertSyntaxError(`for (var {k = do { continue; }} in []) ;`);
assertSyntaxError(`L: for (var {k = do { break L; }} in []) ;`);

assertSyntaxError(`for (var k of do { break; }) ;`);
assertSyntaxError(`for (var k of do { continue; }) ;`);
assertSyntaxError(`L: for (var k of do { break L; }) ;`);

assertSyntaxError(`for (var {k = do { break; }} of []) ;`);
assertSyntaxError(`for (var {k = do { continue; }} of []) ;`);
assertSyntaxError(`L: for (var {k = do { break L; }} of []) ;`);

assertSyntaxError(`for (do { break; }; ; ) ;`);
assertSyntaxError(`for (; do { break; }; ) ;`);
assertSyntaxError(`for (; ; do { break; }) ;`);

assertSyntaxError(`for (do { continue; }; ; ) ;`);
assertSyntaxError(`for (; do { continue; }; ) ;`);
assertSyntaxError(`for (; ; do { continue; }) ;`);

assertSyntaxError(`L: for (do { break L; }; ; ) ;`);
assertSyntaxError(`L: for (; do { break L; }; ) ;`);
assertSyntaxError(`L: for (; ; do { break L; }) ;`);

assertSyntaxError(`switch (do { break; }) {}`);
assertSyntaxError(`L: switch (do { break L; }) {}`);


// break/continue in loop-header (currently) target outer loop.

do {
  while (do { break; }) {
    fail `unreachable`;
  }
  fail `unreachable`;
} while (0);

do {
  while (do { continue; }) {
    fail `unreachable`;
  }
  fail `unreachable`;
} while (0);
