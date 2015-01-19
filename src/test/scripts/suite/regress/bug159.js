/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertThrows
} = Assert;

// assertion in 10.2.1.1.3 step 2 is incorrect
// https://bugs.ecmascript.org/show_bug.cgi?id=159

function nonStrictSet() {
  assertSame("undefined", typeof x);
  assertThrows(ReferenceError, () => x);
  eval("var x; x = (delete x, 0);");
  assertSame(0, x);
  assertTrue(delete x);
  assertSame("undefined", typeof x);
  assertThrows(ReferenceError, () => x);
}
nonStrictSet();

function strictSet() {
  assertSame("undefined", typeof x);
  assertThrows(ReferenceError, () => x);
  assertThrows(ReferenceError, () => eval(`
    var x;
    (function(del) {
      "use strict";
      x = (del(), 0);
    })(() => delete x);
  `));
  assertSame("undefined", typeof x);
  assertThrows(ReferenceError, () => x);
}
strictSet();
