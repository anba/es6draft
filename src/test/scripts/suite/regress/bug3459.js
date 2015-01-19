/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertTrue, fail
} = Assert;

// 24.3.2.4 JA: Incorrect assertion in step 6
// https://bugs.ecmascript.org/show_bug.cgi?id=3459

var calledLength = false;
var p = new Proxy([], {
  get(t, pk, r) {
    if (pk === "length") {
      assertFalse(calledLength);
      calledLength = true;
      return -1;
    }
    if (pk === "toJSON") {
      return;
    }
    fail `unreachable`;
  }
});

assertSame("[]", JSON.stringify(p));
assertTrue(calledLength);
