/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 15.13.6, 15.13.7: TypedArray and DataView objects both use the same internal data property names
// https://bugs.ecmascript.org/show_bug.cgi?id=1670

assertThrows(TypeError, () => DataView.prototype.getInt8.call(new Int8Array(1), 0));
assertThrows(TypeError, () => Int8Array.prototype.set.call(new DataView(new ArrayBuffer(1)), [1], 0));
