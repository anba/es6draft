/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertTrue, assertFalse
} = Assert;

// 24.2.1.2 SetViewValue: Missing call to ToNumber
// https://bugs.ecmascript.org/show_bug.cgi?id=4536

var bb = new ArrayBuffer(1);
var dv = new DataView(bb);
var valueOfCalled = false;

assertThrows(TypeError, () => {
  dv.setInt8(0, {
    valueOf() {
      assertFalse(valueOfCalled);
      valueOfCalled = true;
      detachArrayBuffer(bb);
      return 0;
    }
  });
});

assertTrue(valueOfCalled);
