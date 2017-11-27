/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// 8.1.1.1.5 SetMutableBinding: Missing initialization for replacement binding
// https://bugs.ecmascript.org/show_bug.cgi?id=3528

function f() {
  eval("var x; x = (delete x, 0);");
  assertSame(0, x);
  assertTrue(delete x);
}
f();
