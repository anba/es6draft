/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  fail
} = Assert;

// 22.2.3.22.1 %TypedArray%.prototype.set: Remove length integer validation and use ToLength ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4004

var ta = new Int8Array(10);
var p = new Proxy({}, new Proxy({
  get(t, pk, r) {
    if (pk === "length") {
      return -1;
    }
    fail `unexpected get: ${String(pk)}`;
  }
}, {
  get(t, pk, r) {
    if (pk === "get") {
      return Reflect.get(t, pk, r);
    }
    fail `unexpected trap: ${pk}`;
  }
}));

ta.set(p);
