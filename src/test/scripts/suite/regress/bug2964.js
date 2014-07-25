/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertTrue, assertFalse
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Handle neutered buffers after step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=2964

{
  let constructorCalled = false;
  let buf = new ArrayBuffer(10);
  buf.constructor = function Constructor(len) {
    assertFalse(constructorCalled);
    constructorCalled = true;
    // Internal API call to neuter array buffer
    neuterArrayBuffer(buf);
    return new ArrayBuffer(len);
  };
  assertThrows(() => buf.slice(0), TypeError);
  assertTrue(constructorCalled);
}
