/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 24.2.4.2, 24.2.4.3: Missing checks for neutered buffers in DataView accessors
// https://bugs.ecmascript.org/show_bug.cgi?id=2910

let dv = DataView[Symbol.create]();
assertThrows(() => dv.byteLength, TypeError);
assertThrows(() => dv.byteOffset, TypeError);
assertThrows(() => dv.buffer, TypeError);

let buf = new ArrayBuffer(10);
DataView.call(dv, buf, 2, 6);
assertSame(6, dv.byteLength);
assertSame(2, dv.byteOffset);
assertSame(buf, dv.buffer);

// Internal API call to neuter buffer
neuterArrayBuffer(buf);

assertThrows(() => dv.byteLength, TypeError);
assertThrows(() => dv.byteOffset, TypeError);
assertSame(buf, dv.buffer);
