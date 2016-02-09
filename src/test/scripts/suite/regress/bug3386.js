/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Clarify parsing of function bodies when rest parameter is used
// https://bugs.ecmascript.org/show_bug.cgi?id=3386

function fn(a, b, ...c) {
  assertSame(0, a);
  assertSame(1, b);
  assertSame(2, c.length);
  assertSame(2, c[0]);
  assertSame(3, c[1]);

  assertSame(0, arguments[0]);
  assertSame(1, arguments[1]);
  assertSame(2, arguments[2]);
  assertSame(3, arguments[3]);
  assertSame(4, arguments.length);
}
fn(0, 1, 2, 3);
