/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 22.2.3.2, 22.2.3.3, 22.2.3.17: Incorrect detached buffer checks
// https://bugs.ecmascript.org/show_bug.cgi?id=2908

// let ta = new class extends Int16Array { constructor() { /* no super */ } };
// assertThrows(TypeError, () => ta.byteLength);
// assertThrows(TypeError, () => ta.byteOffset);
// assertThrows(TypeError, () => ta.length);
// assertThrows(TypeError, () => ta.buffer);

let buf = new ArrayBuffer(10);
// Int16Array.call(ta, buf, 2, 3);
let ta = new Int16Array(buf, 2, 3);
assertSame(6, ta.byteLength);
assertSame(2, ta.byteOffset);
assertSame(3, ta.length);
assertSame(buf, ta.buffer);

// Internal API call to detach buffer
detachArrayBuffer(buf);

assertSame(0, ta.byteLength);
assertSame(0, ta.byteOffset);
assertSame(0, ta.length);
assertSame(buf, ta.buffer);
