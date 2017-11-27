/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 24.2.4.2, 24.2.4.3: Missing checks for detached buffers in DataView accessors
// https://bugs.ecmascript.org/show_bug.cgi?id=2910

// let dv = new class extends DataView { constructor() { /* no super */ } };
// assertThrows(TypeError, () => dv.byteLength);
// assertThrows(TypeError, () => dv.byteOffset);
// assertThrows(TypeError, () => dv.buffer);

let buf = new ArrayBuffer(10);
// DataView.call(dv, buf, 2, 6);
let dv = new DataView(buf, 2, 6);
assertSame(6, dv.byteLength);
assertSame(2, dv.byteOffset);
assertSame(buf, dv.buffer);

// Internal API call to detach buffer
detachArrayBuffer(buf);

assertThrows(TypeError, () => dv.byteLength);
assertThrows(TypeError, () => dv.byteOffset);
assertSame(buf, dv.buffer);
