/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 22.2.3.2, 22.2.3.3, 22.2.3.17: Incorrect neutered buffer checks
// https://bugs.ecmascript.org/show_bug.cgi?id=2908

let ta = Int16Array[Symbol.create]();
assertThrows(() => ta.byteLength, TypeError);
assertThrows(() => ta.byteOffset, TypeError);
assertThrows(() => ta.length, TypeError);
assertThrows(() => ta.buffer, TypeError);

let buf = new ArrayBuffer(10);
Int16Array.call(ta, buf, 2, 3);
assertSame(6, ta.byteLength);
assertSame(2, ta.byteOffset);
assertSame(3, ta.length);
assertSame(buf, ta.buffer);

// Internal API call to neuter buffer
neuterArrayBuffer(buf);

assertSame(0, ta.byteLength);
assertSame(0, ta.byteOffset);
assertSame(0, ta.length);
assertSame(buf, ta.buffer);
