/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 24.1.1.5 GetValueFromBuffer, 24.1.1.6 SetValueInBuffer: Assertion in step 1 does not take detached buffers into account
// https://bugs.ecmascript.org/show_bug.cgi?id=3004

let ta = new Int8Array(10);
ta[0] = 10;
assertSame(10, ta[0]);
// Internal API call to detach array buffer
detachArrayBuffer(ta.buffer);
assertThrows(TypeError, () => ta[0]);
assertThrows(TypeError, () => ta[0] = 20);
