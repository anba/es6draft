/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertTrue, assertFalse
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Handle detached buffers after step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=2964

{
  let constructorCalled = false;
  let buf = new ArrayBuffer(10);
  buf.constructor = function Constructor(len) {
    assertFalse(constructorCalled);
    constructorCalled = true;
    // Internal API call to detach array buffer
    detachArrayBuffer(buf);
    return new ArrayBuffer(len);
  };
  buf.constructor[Symbol.species] = buf.constructor;
  assertThrows(TypeError, () => buf.slice(0));
  assertTrue(constructorCalled);
}
