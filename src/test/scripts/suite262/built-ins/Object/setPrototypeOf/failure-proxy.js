/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
description: ...
info: >
  ...
---*/

var callCount = 0;

var obj = {};
var proxy = new Proxy({}, {
  setPrototypeOf: function() {
    callCount += 1;
    return false;
  }
});

assert.throws(TypeError, function() {
  Object.setPrototypeOf(proxy, obj);
});
assert.sameValue(callCount, 1);
