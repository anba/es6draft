/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertTrue, assertFalse
} = Assert;

// 24.1.1.4 CloneArrayBuffer: Handle detached buffers after step 3
// https://bugs.ecmascript.org/show_bug.cgi?id=2963

{
  let buf = new ArrayBuffer(10);
  let ta = new Int8Array(buf);

  // Basic test to ensure call is valid
  ta.set(ta, 0)

  let getterCalled = false;
  Object.defineProperty(buf, "constructor", {
    get() {
      assertFalse(getterCalled);
      getterCalled = true;
      // Internal API call to detach array buffer
      detachArrayBuffer(buf);
      return ArrayBuffer;
    }
  });

  assertThrows(TypeError, () => ta.set(ta, 0));
  assertTrue(getterCalled);
}

{
  let buf = new ArrayBuffer(10);
  let ta = new Int8Array(buf);

  // Basic test to ensure call is valid
  ta.set(ta, 0)

  let getterCalled = false;
  Object.defineProperty(buf, "constructor", {
    get() {
      return Object.defineProperty(function Constructor() { }.bind(null), "prototype", {
        get() {
          assertFalse(getterCalled);
          getterCalled = true;
          // Internal API call to detach array buffer
          detachArrayBuffer(buf);
          return ArrayBuffer.prototype;
        }
      });
    }
  });

  assertThrows(TypeError, () => ta.set(ta, 0));
  assertTrue(getterCalled);
}
