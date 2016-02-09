/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows
} = Assert;

let getProtoCalled = false;
let C = Object.defineProperty(function(){}.bind(), "prototype", {
  get() {
    assertFalse(getProtoCalled);
    getProtoCalled = true;
    return ArrayBuffer.prototype;
  }
});

assertFalse(getProtoCalled);
assertThrows(RangeError, () => Reflect.construct(ArrayBuffer, [Number.MAX_SAFE_INTEGER], C));
assertTrue(getProtoCalled);
