/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, fail
} = Assert;

// 24.3.1.1 Walk: Incorrect assertion in step 3.c.iii and missing ReturnIfAbrupt
// https://bugs.ecmascript.org/show_bug.cgi?id=3458

var calledLength = false;

JSON.parse('{"a": 0, "b": 1}', function reviver(name, value) {
  if (name === "a") {
    this.b = new Proxy([], {
      get(t, pk, r) {
        if (pk === "length") {
          assertFalse(calledLength);
          calledLength = true;
          return -1;
        }
        fail `unreachable`;
      }
    });
  }
  return value;
});

assertTrue(calledLength);
