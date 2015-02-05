/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Array destructoring needs to close its iterators when it is done with them.
// https://bugs.ecmascript.org/show_bug.cgi?id=3576

function* g() {
  try {
    yield 0;
  } finally {
    log = "done";
  }
}

var log = "";
var [] = g();
assertSame("", log);

var log = "";
var [a] = g();
assertSame("done", log);

var log = "";
let [] = g();
assertSame("", log);

var log = "";
let [b] = g();
assertSame("done", log);

var log = "";
[] = g();
assertSame("", log);

var log = "";
var c; [c] = g();
assertSame("done", log);
